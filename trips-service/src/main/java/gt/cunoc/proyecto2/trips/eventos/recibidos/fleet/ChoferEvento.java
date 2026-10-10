package gt.cunoc.proyecto2.trips.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de ChoferCreado y ChoferActualizado (de Fleet), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferEvento(
        UUID choferId,
        UUID sucursalId,
        String nombreCompleto,
        BigDecimal salarioBaseViaje,
        String estado) {
}