package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import gt.cunoc.proyecto2.notification.services.correo.CorreoViajesService;
import org.springframework.stereotype.Component;

/*
 * Avisa al cliente que su alquiler ya tiene bus y piloto (de Trips), y al piloto que se le asignó
 * ese alquiler. Son dos correos distintos: el del cliente viene en el evento y el del piloto se le
 * pide a Identity con su cuenta.
 */
@Component
public class ManejadorAlquilerAsignado implements ManejadorEvento<AlquilerAsignado> {

    private final CorreoVentasService correos;
    private final CorreoViajesService viajes;

    public ManejadorAlquilerAsignado(CorreoVentasService correos, CorreoViajesService viajes) {
        this.correos = correos;
        this.viajes = viajes;
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
        viajes.alquilerAsignadoAlPiloto(evento.eventId(), evento.payload());
    }
}
