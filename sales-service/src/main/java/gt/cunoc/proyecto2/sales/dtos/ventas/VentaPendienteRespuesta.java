package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VentaPendienteRespuesta(
        UUID ventaId,
        EstadoVenta estado,
        BigDecimal montoTotal,
        List<Integer> asientos,
        OffsetDateTime reservadoHasta) {

    public VentaPendienteRespuesta(VentaEntidad venta, List<Integer> asientos, OffsetDateTime reservadoHasta) {
        this(venta.getId(), venta.getEstado(), venta.getMontoTotal(), asientos, reservadoHasta);
    }
}