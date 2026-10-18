package gt.cunoc.proyecto2.sales.eventos.manejadores.wallet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.wallet.MovimientoCartera;
import gt.cunoc.proyecto2.sales.servicios.pagos.PagoCarteraServicio;
import org.springframework.stereotype.Component;

/* CarteraReembolsada (Wallet): la venta cancelada o rechazada queda REEMBOLSADA. */
@Component
public class ManejadorCarteraReembolsada implements ManejadorEvento<MovimientoCartera> {

    private final PagoCarteraServicio pagos;

    public ManejadorCarteraReembolsada(PagoCarteraServicio pagos) {
        this.pagos = pagos;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CARTERA_REEMBOLSADA.nombre();
    }

    @Override
    public Class<MovimientoCartera> tipoPayload() {
        return MovimientoCartera.class;
    }

    @Override
    public void manejar(EventoRecibido<MovimientoCartera> evento) {
        MovimientoCartera movimiento = evento.payload();
        pagos.carteraReembolsada(ReferenciaCartera.valueOf(movimiento.referenciaTipo()), movimiento.referenciaId());
    }
}
