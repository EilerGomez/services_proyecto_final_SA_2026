package gt.cunoc.proyecto2.sales.eventos.manejadores.wallet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.wallet.MovimientoCartera;
import gt.cunoc.proyecto2.sales.servicios.pagos.PagoCarteraServicio;
import org.springframework.stereotype.Component;

/*
 * CarteraDebitada (Wallet), saga: confirma la venta u orden, o pide el reembolso si el débito llegó
 * tarde.
 */
@Component
public class ManejadorCarteraDebitada implements ManejadorEvento<MovimientoCartera> {

    private final PagoCarteraServicio pagos;

    public ManejadorCarteraDebitada(PagoCarteraServicio pagos) {
        this.pagos = pagos;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CARTERA_DEBITADA.nombre();
    }

    @Override
    public Class<MovimientoCartera> tipoPayload() {
        return MovimientoCartera.class;
    }

    @Override
    public void manejar(EventoRecibido<MovimientoCartera> evento) {
        MovimientoCartera movimiento = evento.payload();
        pagos.carteraDebitada(ReferenciaCartera.valueOf(movimiento.referenciaTipo()), movimiento.referenciaId());
    }
}
