package gt.cunoc.proyecto2.identity.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.recibido.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.services.cartera.DebitoCarteraService;
import org.springframework.stereotype.Component;

@Component
public class ManejadorDebitoCartera implements ManejadorEvento<DebitoCarteraSolicitado> {

    private final DebitoCarteraService servicio;

    public ManejadorDebitoCartera(DebitoCarteraService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "DebitoCarteraSolicitado";
    }

    @Override
    public Class<DebitoCarteraSolicitado> tipoPayload() {
        return DebitoCarteraSolicitado.class;
    }

    @Override
    public void manejar(EventoRecibido<DebitoCarteraSolicitado> evento) {
        DebitoCarteraSolicitado payload = evento.payload();
        servicio.procesar(payload.usuarioId(), payload.monto(), payload.referenciaTipo(),
                payload.referenciaId(), evento.businessDate());
    }
}
