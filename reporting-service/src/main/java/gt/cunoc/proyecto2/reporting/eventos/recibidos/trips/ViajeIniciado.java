package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de ViajeIniciado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeIniciado(
        UUID viajeId,
        UUID busId,
        UUID choferId) {
}
