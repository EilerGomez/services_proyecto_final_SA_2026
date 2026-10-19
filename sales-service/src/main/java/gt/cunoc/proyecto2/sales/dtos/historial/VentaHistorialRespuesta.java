package gt.cunoc.proyecto2.sales.dtos.historial;

import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record VentaHistorialRespuesta(
        UUID ventaId,
        CanalVenta canal,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        List<Integer> asientos,
        BigDecimal montoTotal,
        LocalDate fechaPago,
        OffsetDateTime fechaRegistro,
        EstadoVenta estado) {

    public VentaHistorialRespuesta(VentaEntidad venta, VistaViajeEntidad viaje, List<BoletoEntidad> boletos) {
        this(venta.getId(),
                venta.getCanal(),
                viaje == null ? null : viaje.getOrigenNombre(),
                viaje == null ? null : viaje.getDestinoNombre(),
                viaje == null ? null : Fechas.enGuatemala(viaje.getFechaSalida()),
                boletos.stream().map(BoletoEntidad::getNumeroAsiento).sorted(Comparator.naturalOrder()).toList(),
                venta.getMontoTotal(),
                venta.getFechaPago(),
                Fechas.enGuatemala(venta.getFechaCreacion()),
                venta.getEstado());
    }
}