package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Bus y chofer asignados a un alquiler privado, publicado por Trips tras T22.
 * Consume: Notification (avisa al cliente que su viaje ya tiene bus y piloto, y al piloto que se
 * le asignó ese alquiler: para eso viene choferUsuarioId, con el que se le pide el correo a Identity).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
        String busPlaca,
        String choferNombre,
        /** Cuenta del piloto en Identity. Null en eventos de un Trips que todavía no la manda. */
        UUID choferUsuarioId) {
}
