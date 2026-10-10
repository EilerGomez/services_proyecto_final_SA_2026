package gt.cunoc.proyecto2.trips.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import java.util.List;
import java.util.UUID;

/*Alquileres privados: solicitud, cotización, cancelación y asignación del viaje PRIVADO. */
public interface AlquilerServicio {

    AlquilerRespuesta solicitar(NuevoAlquilerSolicitud solicitud, UsuarioActual usuario);

    List<AlquilerRespuesta> listar(FiltroAlquileres filtro);

    List<AlquilerRespuesta> mios(UsuarioActual usuario);

    AlquilerRespuesta obtener(UUID id, UsuarioActual usuario);

    AlquilerRespuesta cotizar(UUID id, CotizarAlquilerSolicitud solicitud, UsuarioActual usuario);

    AlquilerRespuesta cancelar(UUID id, CancelarAlquilerSolicitud solicitud, UsuarioActual usuario);

    ViajeDetalleRespuesta asignar(UUID id, AsignarAlquilerSolicitud solicitud, UsuarioActual usuario);

    void registrarPago(UUID alquilerId);
}