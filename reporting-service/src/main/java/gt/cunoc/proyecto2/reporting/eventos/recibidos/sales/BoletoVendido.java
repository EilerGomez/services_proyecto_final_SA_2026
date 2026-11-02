package gt.cunoc.proyecto2.reporting.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Venta de boletos confirmada en Sales.
 *
 * <p>Incluye la venta, el viaje, la ruta, el bus y la sucursal, además de los datos del cliente y el
 * monto total. Reporting lo usa para los reportes de ingresos y de rutas más demandadas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoletoVendido(
        UUID ventaId,
        UUID viajeId,
        UUID rutaId,
        UUID busId,
        UUID sucursalId,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        Integer cantidadBoletos,
        BigDecimal montoTotal,
        LocalDate fechaPago,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {
}
