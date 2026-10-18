package gt.cunoc.proyecto2.sales.eventos.publicados.cartera;

import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Payload de DebitoCarteraSolicitado (inicio de la saga). Consume: Wallet, que responde
 * CarteraDebitada o DebitoCarteraRechazado. fechaMovimiento = fecha de pago elegida por el usuario.
 */
public record DebitoCarteraSolicitado(
        ReferenciaCartera referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        LocalDate fechaMovimiento) {

    public static DebitoCarteraSolicitado deVenta(VentaEntidad venta) {
        return new DebitoCarteraSolicitado(ReferenciaCartera.VENTA, venta.getId(), venta.getClienteUsuarioId(),
                venta.getMontoTotal(), venta.getFechaPago());
    }

    public static DebitoCarteraSolicitado deOrden(OrdenAlquilerEntidad orden) {
        return new DebitoCarteraSolicitado(ReferenciaCartera.ALQUILER, orden.getId(), orden.getClienteUsuarioId(),
                orden.getMonto(), orden.getFechaPago());
    }
}
