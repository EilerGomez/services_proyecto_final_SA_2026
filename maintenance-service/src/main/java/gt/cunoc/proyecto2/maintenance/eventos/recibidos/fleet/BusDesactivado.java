package gt.cunoc.proyecto2.maintenance.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de BusDesactivado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusDesactivado(UUID busId) {
}
