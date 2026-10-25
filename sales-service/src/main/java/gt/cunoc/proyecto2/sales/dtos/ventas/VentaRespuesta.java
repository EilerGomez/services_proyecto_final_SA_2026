package gt.cunoc.proyecto2.sales.dtos.ventas;

import com.fasterxml.jackson.annotation.JsonInclude;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** fechaRegistro: momento en que se registró la venta (hora de Guatemala); null justo al crearla. */
public record VentaRespuesta(
        UUID ventaId,
        UUID viajeId,
        CanalVenta canal,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        UUID sucursalId,
        MetodoPago metodoPago,
        BigDecimal montoTotal,
        LocalDate fechaPago,
        OffsetDateTime fechaRegistro,
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
                venta.getClienteCorreo(),
                venta.getSucursalId(),
                venta.getMetodoPago(),
                venta.getMontoTotal(),
                venta.getFechaPago(),
                Fechas.enGuatemala(venta.getFechaCreacion()),
                venta.getEstado(),
                venta.getMotivoRechazo(),
                venta.getMotivoCancelacion(),
                venta.getFechaCancelacion(),
                boletos.stream().map(BoletoRespuesta::new).toList(),
                viaje,
                venta.getVersion());
    }
}