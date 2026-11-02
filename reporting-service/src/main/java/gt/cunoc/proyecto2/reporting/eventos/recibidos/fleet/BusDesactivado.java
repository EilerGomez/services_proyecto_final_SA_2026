package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Datos de un bus que Fleet desactiva: solo viaja su identificador. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusDesactivado(UUID busId) {
}
