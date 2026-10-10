package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de RutaCreada y RutaActualizada. Upsert sin versión: gana el último. */
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
