package gt.cunoc.proyecto2.sales.eventos.publicados.cartera;

import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Payload de ReembolsoCarteraSolicitado (compensación). Consume: Wallet, que devuelve el monto
 * y responde CarteraReembolsada. Se pide por venta cancelada, viaje eliminado o débito tardío.
 */
public record ReembolsoCarteraSolicitado(
        ReferenciaCartera referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        MotivoSaga motivo,
        LocalDate fechaMovimiento) {

    public static ReembolsoCarteraSolicitado deVenta(VentaEntidad venta, MotivoSaga motivo, LocalDate fecha) {
        return new ReembolsoCarteraSolicitado(ReferenciaCartera.VENTA, venta.getId(), venta.getClienteUsuarioId(),
                venta.getMontoTotal(), motivo, fecha);
    }

    /**
     * El monto no siempre es el total: si el alquiler se canceló, se devuelve el porcentaje que
     * indicó la tarifa de reembolso. Un cobro de más (débito tardío) sí devuelve todo.
     */
    public static ReembolsoCarteraSolicitado deOrden(OrdenAlquilerEntidad orden, MotivoSaga motivo, LocalDate fecha) {
        return new ReembolsoCarteraSolicitado(ReferenciaCartera.ALQUILER, orden.getId(),
                orden.getClienteUsuarioId(), orden.montoAReembolsar(), motivo, fecha);
    }
}
