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

    ViajeEntidad obtenerPorId(UUID id);

    ViajeRespuesta programar(NuevoViajeSolicitud solicitud, UsuarioActual usuario);

    ViajeRespuesta actualizar(UUID id, ActualizarViajeSolicitud solicitud, UsuarioActual usuario);

    void eliminar(UUID id, String motivo, UsuarioActual usuario);

    VendibleRespuesta consultarVendible(UUID id);

    ViajesActivosRespuesta viajesActivos(UUID busId, UUID choferId);
}