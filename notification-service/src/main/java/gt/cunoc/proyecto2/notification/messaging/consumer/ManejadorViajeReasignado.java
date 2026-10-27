package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeReasignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoViajesService;
import org.springframework.stereotype.Component;

/**
 * Le cambiaron el bus o el piloto a un viaje programado (de Trips). Acá se atiende al cliente de un
 * alquiler privado, cuyo correo viene en el evento; si no lo tiene, no se envía nada. Los pasajeros
 * de un viaje regular llegan por BoletoReasignado, que publica Sales.
 */
@Component
public class ManejadorViajeReasignado implements ManejadorEvento<ViajeReasignado> {

    private final CorreoViajesService correos;

    public ManejadorViajeReasignado(CorreoViajesService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "ViajeReasignado";
    }

    @Override
    public Class<ViajeReasignado> tipoPayload() {
        return ViajeReasignado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeReasignado> evento) {
        correos.viajeReasignado(evento.eventId(), evento.payload());
    }
}
