package gt.cunoc.proyecto2.sales.eventos.manejadores.wallet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.wallet.DebitoCarteraRechazado;
import gt.cunoc.proyecto2.sales.servicios.pagos.PagoCarteraServicio;
import org.springframework.stereotype.Component;

/*
 * DebitoCarteraRechazado (Wallet), saga: rechaza la venta (libera asientos) o la orden (se puede
 * reintentar).
 */
@Component
public class ManejadorDebitoCarteraRechazado implements ManejadorEvento<DebitoCarteraRechazado> {

    private final PagoCarteraServicio pagos;

    public ManejadorDebitoCarteraRechazado(PagoCarteraServicio pagos) {
        this.pagos = pagos;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.DEBITO_CARTERA_RECHAZADO.nombre();
    }

    @Override
    public Class<DebitoCarteraRechazado> tipoPayload() {
        return DebitoCarteraRechazado.class;
    }

    @Override
    public void manejar(EventoRecibido<DebitoCarteraRechazado> evento) {
        DebitoCarteraRechazado rechazo = evento.payload();
        pagos.debitoRechazado(ReferenciaCartera.valueOf(rechazo.referenciaTipo()), rechazo.referenciaId(),
                rechazo.motivo());
    }
}
