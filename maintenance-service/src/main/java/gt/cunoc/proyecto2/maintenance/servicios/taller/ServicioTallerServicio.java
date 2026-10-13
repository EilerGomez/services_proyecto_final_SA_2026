package gt.cunoc.proyecto2.maintenance.servicios.taller;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.NuevoServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.ServicioRespuesta;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ServicioTallerServicio {

    /**
     * Registra un servicio con su detalle y publica GastoTallerRegistrado.
     * Los servicios no se editan ni se eliminan: un error se corrige con otro servicio.
     */
    ServicioRespuesta registrar(NuevoServicioSolicitud solicitud, UsuarioActual usuario);

    List<ServicioRespuesta> listar(UUID busId, LocalDate desde, LocalDate hasta,
                                   UUID categoriaId, UsuarioActual usuario);

    ServicioRespuesta obtener(UUID id, UsuarioActual usuario);
}
