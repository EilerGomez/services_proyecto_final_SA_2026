package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Datos de un chofer que Fleet desactiva: solo viaja su identificador. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferDesactivado(UUID choferId) {
}
