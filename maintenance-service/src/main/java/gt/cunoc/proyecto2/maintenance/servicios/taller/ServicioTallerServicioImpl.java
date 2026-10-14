package gt.cunoc.proyecto2.maintenance.servicios.taller;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.LineaServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.LineaServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.NuevoServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.ServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.errores.ErrorCategoria;
import gt.cunoc.proyecto2.maintenance.errores.ErrorTaller;
import gt.cunoc.proyecto2.maintenance.eventos.PublicadorEventosMaintenance;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.CategoriaRepositorio;
import gt.cunoc.proyecto2.maintenance.repositorios.ServicioRepositorio;
import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioTallerServicioImpl implements ServicioTallerServicio {

    private final ServicioRepositorio servicios;
    private final CategoriaRepositorio categorias;
    private final VistaBusRepositorio buses;
    private final PublicadorEventosMaintenance eventos;
    private final EntityManager entityManager;

    public ServicioTallerServicioImpl(ServicioRepositorio servicios, CategoriaRepositorio categorias,
                                      VistaBusRepositorio buses, PublicadorEventosMaintenance eventos,
                                      EntityManager entityManager) {
        this.servicios = servicios;
        this.categorias = categorias;
        this.buses = buses;
        this.eventos = eventos;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ServicioRespuesta registrar(NuevoServicioSolicitud solicitud, UsuarioActual usuario) {
        VistaBusEntidad bus = buses.findById(solicitud.busId())
                .orElseThrow(() -> ErrorTaller.BUS_NO_EXISTE.excepcion());
        usuario.verificarAccesoSucursal(bus.getSucursalId());
        if (!bus.isActivo()) {
            throw ErrorTaller.BUS_INACTIVO.excepcion();
        }
        Map<UUID, CategoriaEntidad> porId = categoriasPorId(solicitud);

        ServicioTallerEntidad servicio = solicitud.crearEntidad(bus, usuario);
        for (LineaServicioSolicitud linea : solicitud.lineas()) {
            servicio.agregarLinea(solicitud.crearDetalle(linea, porId.get(linea.categoriaId())));
        }
        servicios.saveAndFlush(servicio);   // corre el trigger de totales
        entityManager.refresh(servicio);    // trae total_mano_obra, total_repuestos y total
        eventos.gastoRegistrado(servicio);
        return respuesta(servicio, bus.getPlaca(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listar(UUID busId, LocalDate desde, LocalDate hasta,
                                          UUID categoriaId, UsuarioActual usuario) {
        List<ServicioTallerEntidad> encontrados =
                servicios.buscar(sucursalDelFiltro(usuario), busId, desde, hasta, categoriaId);
        // solo las placas de los buses del resultado (no toda la vista)
        Set<UUID> busIds = new HashSet<>();
        encontrados.forEach(s -> busIds.add(s.getBusId()));
        Map<UUID, String> placas = new HashMap<>();
        buses.findAllById(busIds).forEach(b -> placas.put(b.getBusId(), b.getPlaca()));
        return encontrados.stream()
                .map(s -> respuesta(s, placas.getOrDefault(s.getBusId(), "-"), false))
                .toList();
    }

    /**
     * ADMIN_SISTEMA ve todas las sucursales (null); los demás solo la suya.
     * Si el token no trae sucursalId → 403 SUCURSAL_AJENA (no se cae a "todas").
     */
    private static UUID sucursalDelFiltro(UsuarioActual usuario) {
        if (usuario.esAdminSistema()) {
            return null;
        }
        usuario.verificarAccesoSucursal(usuario.sucursalId());   // con sucursalId null lanza ProhibidoException
        return usuario.sucursalId();
    }

    @Override
    @Transactional(readOnly = true)
    public ServicioRespuesta obtener(UUID id, UsuarioActual usuario) {
        ServicioTallerEntidad servicio = servicios.findById(id)
                .orElseThrow(() -> ErrorTaller.SERVICIO_NO_EXISTE.excepcion());
        usuario.verificarAccesoSucursal(servicio.getSucursalId());
        String placa = buses.findById(servicio.getBusId())
                .map(VistaBusEntidad::getPlaca).orElse("-");
        return respuesta(servicio, placa, true);
    }

    private Map<UUID, CategoriaEntidad> categoriasPorId(NuevoServicioSolicitud solicitud) {
        Map<UUID, CategoriaEntidad> porId = new HashMap<>();
        for (LineaServicioSolicitud linea : solicitud.lineas()) {
            porId.computeIfAbsent(linea.categoriaId(), id -> categorias.findById(id)
                    .orElseThrow(() -> ErrorCategoria.CATEGORIA_NO_EXISTE.excepcion()));
        }
        return porId;
    }

    private ServicioRespuesta respuesta(ServicioTallerEntidad servicio, String placa, boolean conLineas) {
        List<LineaServicioRespuesta> lineas = conLineas
                ? servicio.getLineas().stream().map(LineaServicioRespuesta::new).toList()
                : null;
        return new ServicioRespuesta(servicio, placa, lineas);
    }
}
