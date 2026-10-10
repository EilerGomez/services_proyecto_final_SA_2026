package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de ViajeEliminado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeEliminado(UUID viajeId) {
}
