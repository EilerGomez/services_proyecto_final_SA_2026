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
 * Payload de VentaReembolsada (S14): la sucursal devolvió en efectivo el dinero de una venta cancelada.
 * Consume: Notification (avisa al cliente si la venta se registró con correo).
 * Reporting no lo consume: el ingreso ya se restó al cancelar la venta (VentaCancelada), y devolver
 * el efectivo no cambia las cuentas otra vez.
 * origenNombre, destinoNombre y fechaSalida son null si el viaje ya no está en vista_viajes; en ese
 * caso el viaje se reconoce por el motivo, que lo lleva escrito.
 */
public record VentaReembolsada(
        UUID ventaId,
        String codigo,
        UUID viajeId,
        UUID sucursalId,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        int cantidadBoletos,
        BigDecimal montoReembolsado,
        MetodoPago metodoPago,
        String motivo,
        LocalDate fechaReembolso,
        UUID reembolsadoPor,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {

    public static VentaReembolsada desde(VentaEntidad venta, List<BoletoEntidad> boletos,
                                         VistaViajeEntidad viaje) {
        List<Integer> asientos = boletos.stream().map(BoletoEntidad::getNumeroAsiento).sorted().toList();
        return new VentaReembolsada(venta.getId(), venta.codigo(), venta.getViajeId(), venta.getSucursalId(),
                venta.getClienteUsuarioId(), venta.getClienteNit(), venta.getClienteNombre(),
                venta.getClienteCorreo(), asientos, asientos.size(), venta.getMontoTotal(),
                venta.getMetodoPago(), venta.getMotivoCancelacion(), venta.getFechaReembolso(),
                venta.getReembolsadoPor(),
                viaje == null ? null : viaje.getOrigenNombre(),
                viaje == null ? null : viaje.getDestinoNombre(),
                viaje == null ? null : Fechas.enGuatemala(viaje.getFechaSalida()));
    }
}
