package gt.cunoc.proyecto2.trips.servicios.viajes;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.viajes.ActualizarViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.dtos.viajes.NuevoViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.VendibleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.util.List;
import java.util.UUID;

public interface ViajeServicio {

    List<ViajeRespuesta> buscar(FiltroViajes filtro);

    ViajeDetalleRespuesta obtener(UUID id, UsuarioActual usuario);

    ViajeDetalleRespuesta detalle(ViajeEntidad viaje);

    /** Datos del viaje sin su salida ni llegada (para incrustarlos en la respuesta de un alquiler). */
    ViajeRespuesta resumen(ViajeEntidad viaje);

    ViajeEntidad obtenerPorId(UUID id);

    ViajeRespuesta programar(NuevoViajeSolicitud solicitud, UsuarioActual usuario);

    ViajeRespuesta actualizar(UUID id, ActualizarViajeSolicitud solicitud, UsuarioActual usuario);

    void eliminar(UUID id, String motivo, UsuarioActual usuario);

    /*
     * Borra el viaje PRIVADO de un alquiler que se está cancelando y devuelve su id (null si el
     * alquiler no tenía bus asignado). No valida roles ni sucursal: el permiso ya se verificó sobre el
     * alquiler, que es lo que el usuario está cancelando —y desde PAGADO el propio cliente puede
     * hacerlo, aunque no sea personal de la sucursal—. Tampoco libera el alquiler (no vuelve a PAGADO
     * como cuando se borra el viaje para cambiarle la unidad): quien llama lo deja CANCELADO.
     *
     * @throws gt.cunoc.proyecto2.comun.errores.NegocioException 422 si el viaje ya salió
     */
    UUID eliminarPorCancelacionDeAlquiler(UUID alquilerId, String motivo);

    VendibleRespuesta consultarVendible(UUID id);

    ViajesActivosRespuesta viajesActivos(UUID busId, UUID choferId);
}