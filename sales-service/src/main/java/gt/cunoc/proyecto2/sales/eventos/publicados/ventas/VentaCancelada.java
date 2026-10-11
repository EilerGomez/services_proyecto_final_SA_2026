package gt.cunoc.proyecto2.sales.eventos.publicados.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Payload de VentaCancelada (S5 o ViajeEliminado).
 * Consumen: Reporting (resta el ingreso en la fecha de cancelación) y Notification (avisa al cliente).
 * origenNombre, destinoNombre y fechaSalida son null si el viaje ya no está en vista_viajes.
 */
public record VentaCancelada(
        UUID ventaId,
        UUID viajeId,
        UUID sucursalId,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        int cantidadBoletos,
        BigDecimal montoAnulado,
        MetodoPago metodoPago,
        String motivo,
        LocalDate fechaCancelacion,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {

    public static VentaCancelada desde(VentaEntidad venta, List<BoletoEntidad> boletos, VistaViajeEntidad viaje) {
        List<Integer> asientos = boletos.stream().map(BoletoEntidad::getNumeroAsiento).sorted().toList();
        return new VentaCancelada(venta.getId(), venta.getViajeId(), venta.getSucursalId(),
                venta.getClienteUsuarioId(), venta.getClienteNit(), venta.getClienteNombre(),
                venta.getClienteCorreo(), asientos, asientos.size(), venta.getMontoTotal(), venta.getMetodoPago(),
                venta.getMotivoCancelacion(), venta.getFechaCancelacion(),
                viaje == null ? null : viaje.getOrigenNombre(),
                viaje == null ? null : viaje.getDestinoNombre(),
                viaje == null ? null : Fechas.enGuatemala(viaje.getFechaSalida()));
    }
}
