package gt.cunoc.proyecto2.reporting.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Pago de un alquiler privado confirmado en Sales.
 *
 * <p>Trae el monto cobrado, el alquiler y la sucursal que lo atendió, además del rango de fechas en
 * que el vehículo estuvo rentado. Reporting lo guarda como ingreso de tipo alquiler.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlquilerPagado(
        UUID ordenId,
        UUID alquilerId,
        UUID sucursalId,
        String clienteNit,
        String clienteNombre,
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        BigDecimal monto,
        BigDecimal montoTotal,
        LocalDate fechaPago) {
}
