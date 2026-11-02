package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cierre de un viaje, con los kilómetros recorridos y los costos ya calculados por Trips.
 *
 * <p>Reporting usa el costo de combustible, la depreciación y el salario que vienen en el evento, no
 * la tarifa vigente en su propia tabla; así el gasto histórico no cambia si luego se ajusta la
 * tarifa.
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
