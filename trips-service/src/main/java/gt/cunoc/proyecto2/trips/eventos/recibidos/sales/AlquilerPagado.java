package gt.cunoc.proyecto2.trips.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de AlquilerPagado (de Sales), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlquilerPagado(
        UUID ordenId,
        UUID alquilerId) {
}