package gt.cunoc.proyecto2.trips.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de BusDesactivado (de Fleet), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusDesactivado(UUID busId) {
}