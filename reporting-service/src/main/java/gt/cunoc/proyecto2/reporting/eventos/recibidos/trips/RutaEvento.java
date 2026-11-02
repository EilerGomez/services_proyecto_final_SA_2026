package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de una ruta en los eventos de creación y actualización de Trips.
 *
 * <p>El evento no trae número de versión, así que la proyección simplemente sobrescribe la ruta: el
 * último mensaje que llega es el que queda.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaEvento(
        UUID rutaId,
        UUID sucursalOrigenId,
        UUID sucursalDestinoId,
        String origenNombre,
        String destinoNombre,
        BigDecimal distanciaKm,
        BigDecimal precioBoleto,
        Boolean activa) {
}
