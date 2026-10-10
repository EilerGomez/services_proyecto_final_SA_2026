package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de RutaEliminada. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaEliminada(UUID rutaId) {
}
