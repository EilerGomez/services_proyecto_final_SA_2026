package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import org.springframework.stereotype.Component;

/*
 * Avisa al cliente que su alquiler ya tiene bus y piloto (de Trips).
 */
@Component
public class ManejadorAlquilerAsignado implements ManejadorEvento<AlquilerAsignado> {

    private final CorreoVentasService correos;

    public ManejadorAlquilerAsignado(CorreoVentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "AlquilerAsignado";
    }

    @Override
    public Class<AlquilerAsignado> tipoPayload() {
        return AlquilerAsignado.class;
    }

    @Override
    public void manejar(EventoRecibido<AlquilerAsignado> evento) {
        correos.alquilerAsignado(evento.eventId(), evento.payload());
    }
}
