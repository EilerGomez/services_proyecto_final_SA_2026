package gt.cunoc.proyecto2.trips.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de ChoferDesactivado (de Fleet), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferDesactivado(UUID choferId) {
}