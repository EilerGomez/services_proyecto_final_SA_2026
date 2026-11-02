package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Datos de una ruta que Trips elimina: solo viaja su identificador. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaEliminada(UUID rutaId) {
}
