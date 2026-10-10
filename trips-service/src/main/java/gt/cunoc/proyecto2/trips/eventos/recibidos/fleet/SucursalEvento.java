package gt.cunoc.proyecto2.trips.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de SucursalCreada y SucursalActualizada (de Fleet), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SucursalEvento(
        UUID sucursalId,
        String nombre,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activa) {
}