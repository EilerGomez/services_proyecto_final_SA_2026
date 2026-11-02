package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Arranque de un viaje: identifica el viaje, el bus y el chofer que salieron a ruta.
 *
 * <p>Reporting lo usa para cambiar el estado del viaje y para dejar ese chofer como el actual del
 * bus.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeIniciado(
        UUID viajeId,
        UUID busId,
        UUID choferId) {
}
