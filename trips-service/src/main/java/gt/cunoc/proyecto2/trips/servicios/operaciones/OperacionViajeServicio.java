package gt.cunoc.proyecto2.trips.servicios.operaciones;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarLlegadaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarSalidaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import java.util.UUID;

/*
 * Salida y llegada de un viaje (REGULAR o PRIVADO). Las registra el chofer asignado o un
 * ADMIN_SUCURSAL de la sucursal del viaje (403 CHOFER_NO_ASIGNADO / SUCURSAL_AJENA).
 */
public interface OperacionViajeServicio {

    ViajeDetalleRespuesta registrarSalida(UUID viajeId, RegistrarSalidaSolicitud solicitud, UsuarioActual usuario);

    ViajeDetalleRespuesta registrarLlegada(UUID viajeId, RegistrarLlegadaSolicitud solicitud, UsuarioActual usuario);
}