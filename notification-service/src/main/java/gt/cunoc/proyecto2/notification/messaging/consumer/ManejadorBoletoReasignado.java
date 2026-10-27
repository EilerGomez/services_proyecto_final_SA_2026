package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoReasignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoViajesService;
import org.springframework.stereotype.Component;

/**
 * Avisa al pasajero de un viaje regular que le cambiaron el bus o el piloto (de Sales, que publica
 * uno por cada boleto confirmado con correo).
 */
@Component
public class ManejadorBoletoReasignado implements ManejadorEvento<BoletoReasignado> {

    private final CorreoViajesService correos;

    public ManejadorBoletoReasignado(CorreoViajesService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "BoletoReasignado";
    }

    @Override
    public Class<BoletoReasignado> tipoPayload() {
        return BoletoReasignado.class;
    }

    @Override
    public void manejar(EventoRecibido<BoletoReasignado> evento) {
        correos.boletoReasignado(evento.eventId(), evento.payload());
    }
}
