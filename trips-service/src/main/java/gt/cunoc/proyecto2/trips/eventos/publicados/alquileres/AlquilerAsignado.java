package gt.cunoc.proyecto2.trips.eventos.publicados.alquileres;

import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Payload de AlquilerAsignado (T22): el alquiler pagado ya tiene bus y chofer.
 * Consume: Notification (avisa al cliente que su viaje privado ya tiene bus y piloto, y al piloto
 * que le tocó ese alquiler: por eso va también su cuenta de Identity).
 * clienteCorreo puede venir null si el alquiler se pidió en mostrador sin correo.
 */
public record AlquilerAsignado(
        UUID alquilerId,
        UUID sucursalId,
        UUID viajeId,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        UUID busId,
        String busPlaca,
        UUID choferId,
        String choferNombre,
        /** Cuenta del piloto en Identity, para avisarle a él también. Puede ser null. */
        UUID choferUsuarioId) {

    public static AlquilerAsignado desde(AlquilerPrivadoEntidad alquiler, ViajeRespuesta viaje) {
        return new AlquilerAsignado(alquiler.getId(), alquiler.getSucursalId(), viaje.viajeId(),
                alquiler.getClienteNombre(), alquiler.getClienteCorreo(), alquiler.getOrigen(), alquiler.getDestino(),
                Fechas.enGuatemala(alquiler.getFechaSalida()), Fechas.enGuatemala(alquiler.getFechaRetorno()),
                viaje.busId(), viaje.busPlaca(), viaje.choferId(), viaje.choferNombre(),
                viaje.choferUsuarioId());
    }
}
