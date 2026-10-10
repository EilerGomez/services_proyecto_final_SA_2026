package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de ChoferDesactivado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferDesactivado(UUID choferId) {
}
