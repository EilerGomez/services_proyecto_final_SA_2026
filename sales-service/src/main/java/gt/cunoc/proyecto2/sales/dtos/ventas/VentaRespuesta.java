package gt.cunoc.proyecto2.sales.dtos.ventas;

import com.fasterxml.jackson.annotation.JsonInclude;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VentaRespuesta(
        UUID ventaId,
        UUID viajeId,
        CanalVenta canal,
        String clienteNit,
        String clienteNombre,
        UUID sucursalId,
        MetodoPago metodoPago,
        BigDecimal montoTotal,
        LocalDate fechaPago,
        EstadoVenta estado,
        String motivoRechazo,
        String motivoCancelacion,
        LocalDate fechaCancelacion,
        List<BoletoRespuesta> boletos,
        @JsonInclude(JsonInclude.Include.NON_NULL) ViajeResumenRespuesta viaje,
        Integer version) {

    public VentaRespuesta(VentaEntidad venta, List<BoletoEntidad> boletos, ViajeResumenRespuesta viaje) {
        this(venta.getId(),
                venta.getViajeId(),
                venta.getCanal(),
                venta.getClienteNit(),
                venta.getClienteNombre(),
                venta.getSucursalId(),
                venta.getMetodoPago(),
                venta.getMontoTotal(),
                venta.getFechaPago(),
                venta.getEstado(),
                venta.getMotivoRechazo(),
                venta.getMotivoCancelacion(),
                venta.getFechaCancelacion(),
                boletos.stream().map(BoletoRespuesta::new).toList(),
                viaje,
                venta.getVersion());
    }
}