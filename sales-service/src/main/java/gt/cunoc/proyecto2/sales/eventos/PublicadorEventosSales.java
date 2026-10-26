package gt.cunoc.proyecto2.sales.eventos;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.sales.eventos.publicados.alquileres.AlquilerPagado;
import gt.cunoc.proyecto2.sales.eventos.publicados.cartera.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.sales.eventos.publicados.cartera.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.sales.eventos.publicados.ventas.BoletoVendido;
import gt.cunoc.proyecto2.sales.eventos.publicados.ventas.VentaCancelada;
import gt.cunoc.proyecto2.sales.eventos.publicados.ventas.VentaReembolsada;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Los 6 eventos que publica Sales, con su tipo de agregado, aggregateId, aggregateVersion y businessDate.
 * Los servicios llaman a estos métodos en lugar de escribir nombres de eventos a mano.
 *
 * <p>Reglas (comun): se llama dentro de la transacción del cambio y después de saveAndFlush,
 * para que la version de la entidad ya esté incrementada. El evento queda en eventos_salida (outbox)
 * y RelayOutbox lo envía a sales.eventos: si la transacción se revierte, el evento tampoco sale.</p>
 *
 * <p>businessDate (04_sales.md): débito, boleto vendido y alquiler pagado = fecha de pago;
 * venta cancelada = fecha de cancelación; venta reembolsada = fecha del reembolso;
 * reembolso = la fecha que indique quien lo pide.</p>
 */
@Component
public class PublicadorEventosSales {

    private final PublicadorEventos publicador;

    public PublicadorEventosSales(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    // ----- Saga con Wallet -----

    /** S2: cobra la venta en línea con la cartera del cliente. */
    public void debitoVenta(VentaEntidad venta) {
        publicar(TipoAgregado.VENTA, venta.getId(), venta.getVersion(), TipoEvento.DEBITO_CARTERA_SOLICITADO,
                DebitoCarteraSolicitado.deVenta(venta), venta.getFechaPago());
    }

    /** S12: cobra la orden de alquiler con la cartera del cliente. */
    public void debitoOrden(OrdenAlquilerEntidad orden) {
        publicar(TipoAgregado.ORDEN_ALQUILER, orden.getId(), orden.getVersion(),
                TipoEvento.DEBITO_CARTERA_SOLICITADO, DebitoCarteraSolicitado.deOrden(orden), orden.getFechaPago());
    }

    public void reembolsoVenta(VentaEntidad venta, MotivoSaga motivo, LocalDate fecha) {
        publicar(TipoAgregado.VENTA, venta.getId(), venta.getVersion(), TipoEvento.REEMBOLSO_CARTERA_SOLICITADO,
                ReembolsoCarteraSolicitado.deVenta(venta, motivo, fecha), fecha);
    }

    public void reembolsoOrden(OrdenAlquilerEntidad orden, MotivoSaga motivo, LocalDate fecha) {
        publicar(TipoAgregado.ORDEN_ALQUILER, orden.getId(), orden.getVersion(),
                TipoEvento.REEMBOLSO_CARTERA_SOLICITADO, ReembolsoCarteraSolicitado.deOrden(orden, motivo, fecha),
                fecha);
    }

    // ----- Ventas -----

    /** S4 o CarteraDebitada: la venta quedó CONFIRMADA con sus boletos. */
    public void boletoVendido(VentaEntidad venta, List<BoletoEntidad> boletos, VistaViajeEntidad viaje) {
        publicar(TipoAgregado.VENTA, venta.getId(), venta.getVersion(), TipoEvento.BOLETO_VENDIDO,
                BoletoVendido.desde(venta, boletos, viaje), venta.getFechaPago());
    }

    /** S5 o ViajeEliminado. viaje puede ser null si ya no está en vista_viajes. */
    public void ventaCancelada(VentaEntidad venta, List<BoletoEntidad> boletos, VistaViajeEntidad viaje) {
        publicar(TipoAgregado.VENTA, venta.getId(), venta.getVersion(), TipoEvento.VENTA_CANCELADA,
                VentaCancelada.desde(venta, boletos, viaje), venta.getFechaCancelacion());
    }

    /** S14: la sucursal devolvió el efectivo de una venta cancelada. viaje puede ser null. */
    public void ventaReembolsada(VentaEntidad venta, List<BoletoEntidad> boletos, VistaViajeEntidad viaje) {
        publicar(TipoAgregado.VENTA, venta.getId(), venta.getVersion(), TipoEvento.VENTA_REEMBOLSADA,
                VentaReembolsada.desde(venta, boletos, viaje), venta.getFechaReembolso());
    }

    // ----- Alquileres -----

    /** S13 o CarteraDebitada: la orden quedó PAGADA. */
    public void alquilerPagado(OrdenAlquilerEntidad orden) {
        publicar(TipoAgregado.ORDEN_ALQUILER, orden.getId(), orden.getVersion(), TipoEvento.ALQUILER_PAGADO,
                AlquilerPagado.desde(orden), orden.getFechaPago());
    }

    private void publicar(TipoAgregado agregado, UUID agregadoId, Integer version, TipoEvento evento,
                          Object payload, LocalDate fecha) {
        publicador.publicar(agregado.nombre(), agregadoId, version == null ? null : version.longValue(),
                evento.nombre(), payload, fecha);
    }
}
