package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Datos de un viaje que Trips elimina: solo viaja su identificador. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeEliminado(UUID viajeId) {
}
