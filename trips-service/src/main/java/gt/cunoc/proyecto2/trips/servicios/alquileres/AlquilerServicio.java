package gt.cunoc.proyecto2.trips.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.PoliticaCancelacionRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*Alquileres privados: solicitud, cotización, cancelación y asignación del viaje PRIVADO. */
public interface AlquilerServicio {

    AlquilerRespuesta solicitar(NuevoAlquilerSolicitud solicitud, UsuarioActual usuario);

    List<AlquilerRespuesta> listar(FiltroAlquileres filtro);

    List<AlquilerRespuesta> mios(UsuarioActual usuario);

    AlquilerRespuesta obtener(UUID id, UsuarioActual usuario);

    AlquilerRespuesta cotizar(UUID id, CotizarAlquilerSolicitud solicitud, UsuarioActual usuario);

    /**
     * T21. Cancela el alquiler, borra su viaje PRIVADO si ya tenía bus y publica AlquilerCancelado
     * para que Sales cancele la orden y devuelva el porcentaje de la tarifa que aplique.
     * Lo puede hacer el cliente dueño o el administrador de la sucursal.
     *
     * <ul>
     *   <li>403 ALQUILER_AJENO si no es ninguno de los dos.</li>
     *   <li>409 si la version no coincide.</li>
     *   <li>422 ESTADO_NO_CANCELABLE si el alquiler ya está COMPLETADO o CANCELADO.</li>
     *   <li>422 CANCELACION_NO_PERMITIDA si la tarifa vigente no permite cancelar a esa hora; en ese
     *       caso no se toca ni el alquiler ni el viaje.</li>
     * </ul>
     */
    AlquilerRespuesta cancelar(UUID id, CancelarAlquilerSolicitud solicitud, UsuarioActual usuario);

    /**
     * Qué pasaría si este alquiler se cancelara en ese momento (o ahora, si no se indica): si se
     * permite, qué porcentaje se devolvería y por qué. Lo consulta el frontend para el aviso al
     * cliente y para el diálogo de confirmación.
     */
    PoliticaCancelacionRespuesta politicaDeCancelacion(UUID id, OffsetDateTime cuando, UsuarioActual usuario);

    ViajeDetalleRespuesta asignar(UUID id, AsignarAlquilerSolicitud solicitud, UsuarioActual usuario);

    void registrarPago(UUID alquilerId);
}