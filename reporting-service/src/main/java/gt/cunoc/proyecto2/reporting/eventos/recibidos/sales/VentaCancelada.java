package gt.cunoc.proyecto2.reporting.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cancelación de una venta de boletos, con el monto que se devuelve al cliente.
 *
 * <p>No trae ruta, bus ni origen/destino: Reporting los copia de la fila positiva de la misma venta
 * y registra el reembolso como un ingreso negativo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaCancelada(
        UUID ventaId,
        UUID viajeId,
        UUID sucursalId,
        String clienteNit,
        String clienteNombre,
        Integer cantidadBoletos,
        BigDecimal montoAnulado) {
}
