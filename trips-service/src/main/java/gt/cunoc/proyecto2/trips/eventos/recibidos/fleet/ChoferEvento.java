package gt.cunoc.proyecto2.trips.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de ChoferCreado y ChoferActualizado (de Fleet), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferEvento(
        UUID choferId,
        /** Su cuenta en Identity: con ella Notification le escribe cuando se le asigna un viaje. */
        UUID usuarioId,
        UUID sucursalId,
        String nombreCompleto,
        BigDecimal salarioBaseViaje,
        String estado) {
}