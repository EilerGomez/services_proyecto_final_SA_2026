package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeProgramado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoViajesService;
import org.springframework.stereotype.Component;

/**
 * Se programó un viaje en Trips: se le avisa por correo al piloto que le tocó. Solo ViajeProgramado,
 * no ViajeActualizado: cuando a un viaje le cambian el bus o el piloto, el aviso sale por
 * ViajeReasignado, que es el que sabe qué cambió.
 */
@Component
public class ManejadorViajeProgramado implements ManejadorEvento<ViajeProgramado> {

    private final CorreoViajesService correos;

    public ManejadorViajeProgramado(CorreoViajesService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "ViajeProgramado";
    }

    @Override
    public Class<ViajeProgramado> tipoPayload() {
        return ViajeProgramado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeProgramado> evento) {
        correos.viajeAsignadoAlPiloto(evento.eventId(), evento.payload());
    }
}
