package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.VentaCancelada;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import org.springframework.stereotype.Component;

/**
 * Avisa que la compra fue cancelada y si el monto vuelve a la cartera (de Sales).
 */
@Component
public class ManejadorVentaCancelada implements ManejadorEvento<VentaCancelada> {

    private final CorreoVentasService correos;

    public ManejadorVentaCancelada(CorreoVentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "VentaCancelada";
    }

    @Override
    public Class<VentaCancelada> tipoPayload() {
        return VentaCancelada.class;
    }

    @Override
    public void manejar(EventoRecibido<VentaCancelada> evento) {
        correos.ventaCancelada(evento.eventId(), evento.payload());
    }
}