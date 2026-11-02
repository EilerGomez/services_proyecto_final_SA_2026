package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Datos de un viaje en los eventos de programación y actualización de Trips.
 *
 * <p>Indica la ruta, el bus, el chofer y la fecha de salida. El alquiler solo viene cuando el viaje
 * es privado; en un viaje regular llega nulo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeEvento(
        UUID viajeId,
        String tipo,
        UUID rutaId,
        UUID alquilerId,
        UUID sucursalId,
        UUID busId,
        UUID choferId,
        OffsetDateTime fechaSalida) {
}
