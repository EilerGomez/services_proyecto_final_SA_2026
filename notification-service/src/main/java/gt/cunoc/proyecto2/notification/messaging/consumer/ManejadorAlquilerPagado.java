package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerPagado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import org.springframework.stereotype.Component;

/*
 * Confirma el pago de un alquiler privado (de Sales).
 */
@Component
public class ManejadorAlquilerPagado implements ManejadorEvento<AlquilerPagado> {

    private final CorreoVentasService correos;

    public ManejadorAlquilerPagado(CorreoVentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "AlquilerPagado";
    }

    @Override
    public Class<AlquilerPagado> tipoPayload() {
        return AlquilerPagado.class;
    }

    @Override
    public void manejar(EventoRecibido<AlquilerPagado> evento) {
        correos.alquilerPagado(evento.eventId(), evento.payload());
    }
}