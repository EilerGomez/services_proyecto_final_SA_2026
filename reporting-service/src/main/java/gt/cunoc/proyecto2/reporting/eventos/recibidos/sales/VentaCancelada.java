package gt.cunoc.proyecto2.reporting.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de VentaCancelada. No trae ruta, bus ni origen/destino: se copian de la fila
 * positiva de la misma venta.
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
