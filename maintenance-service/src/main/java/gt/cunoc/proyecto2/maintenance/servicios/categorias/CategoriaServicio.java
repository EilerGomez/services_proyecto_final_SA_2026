package gt.cunoc.proyecto2.maintenance.servicios.categorias;

import gt.cunoc.proyecto2.maintenance.dtos.categorias.ActualizarCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.CategoriaRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.NuevaCategoriaSolicitud;
import java.util.List;
import java.util.UUID;

public interface CategoriaServicio {

    List<CategoriaRespuesta> listar(Boolean activa);

    CategoriaRespuesta obtener(UUID id);

    CategoriaRespuesta crear(NuevaCategoriaSolicitud solicitud);

    CategoriaRespuesta actualizar(UUID id, ActualizarCategoriaSolicitud solicitud);
}
