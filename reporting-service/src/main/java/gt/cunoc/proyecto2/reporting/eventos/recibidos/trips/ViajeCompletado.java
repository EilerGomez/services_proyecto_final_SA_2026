package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de ViajeCompletado. El costo de cada viaje usa la tarifa que guardó Trips
 * (no la vigente en reporting).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeCompletado(
        UUID viajeId,
        String tipo,
        UUID rutaId,
        UUID sucursalId,
        UUID busId,
        UUID choferId,
        BigDecimal kmRecorridos,
        BigDecimal costoCombustible,
        BigDecimal tarifaDepreciacion,
        BigDecimal costoDepreciacion,
        BigDecimal salarioChofer,
        OffsetDateTime fechaHoraLlegada) {
}
