package gt.cunoc.proyecto2.wallet.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.wallet.evento.recibido.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.wallet.services.ReembolsoCarteraService;
import org.springframework.stereotype.Component;

/** ReembolsoCarteraSolicitado (Sales): compensación de la saga. */
@Component
public class ManejadorReembolsoCartera implements ManejadorEvento<ReembolsoCarteraSolicitado> {

    private final ReembolsoCarteraService servicio;

    public ManejadorReembolsoCartera(ReembolsoCarteraService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "ReembolsoCarteraSolicitado";
    }

    @Override
    public Class<ReembolsoCarteraSolicitado> tipoPayload() {
        return ReembolsoCarteraSolicitado.class;
    }

    @Override
    public void manejar(EventoRecibido<ReembolsoCarteraSolicitado> evento) {
        ReembolsoCarteraSolicitado payload = evento.payload();
        servicio.procesar(payload.usuarioId(), payload.monto(), payload.referenciaTipo(),
                payload.referenciaId(), evento.businessDate(), payload.motivo());
    }
}
