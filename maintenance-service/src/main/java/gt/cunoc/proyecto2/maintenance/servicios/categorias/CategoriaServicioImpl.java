package gt.cunoc.proyecto2.maintenance.servicios.categorias;

import gt.cunoc.proyecto2.maintenance.dtos.categorias.ActualizarCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.CategoriaRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.NuevaCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.errores.ErrorCategoria;
import gt.cunoc.proyecto2.maintenance.eventos.PublicadorEventosMaintenance;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.CategoriaRepositorio;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaServicioImpl implements CategoriaServicio {

    private final CategoriaRepositorio categorias;
    private final PublicadorEventosMaintenance eventos;

    public CategoriaServicioImpl(CategoriaRepositorio categorias, PublicadorEventosMaintenance eventos) {
        this.categorias = categorias;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listar(Boolean activa) {
        return categorias.findAllByOrderByNombreAsc().stream()
                .filter(c -> activa == null || c.isActiva() == activa)
                .map(CategoriaRespuesta::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaRespuesta obtener(UUID id) {
        return new CategoriaRespuesta(buscar(id));
    }

    @Override
    @Transactional
    public CategoriaRespuesta crear(NuevaCategoriaSolicitud solicitud) {
        if (categorias.existsByNombreIgnoreCase(solicitud.nombre())) {
            throw ErrorCategoria.CATEGORIA_DUPLICADA.excepcion(solicitud.nombre());
        }
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setNombre(solicitud.nombre());
        categoria.setDescripcion(solicitud.descripcion());
        categoria.setActiva(true);
        categorias.save(categoria);
        eventos.categoriaActualizada(categoria);
        return new CategoriaRespuesta(categoria);
    }

    @Override
    @Transactional
    public CategoriaRespuesta actualizar(UUID id, ActualizarCategoriaSolicitud solicitud) {
        CategoriaEntidad categoria = buscar(id);
        if (!categoria.getNombre().equalsIgnoreCase(solicitud.nombre())
                && categorias.existsByNombreIgnoreCase(solicitud.nombre())) {
            throw ErrorCategoria.CATEGORIA_DUPLICADA.excepcion(solicitud.nombre());
        }
        categoria.setNombre(solicitud.nombre());
        categoria.setDescripcion(solicitud.descripcion());
        if (solicitud.activa() != null) {
            categoria.setActiva(solicitud.activa());
        }
        categorias.save(categoria);
        eventos.categoriaActualizada(categoria);
        return new CategoriaRespuesta(categoria);
    }

    private CategoriaEntidad buscar(UUID id) {
        return categorias.findById(id)
                .orElseThrow(() -> ErrorCategoria.CATEGORIA_NO_EXISTE.excepcion());
    }
}
