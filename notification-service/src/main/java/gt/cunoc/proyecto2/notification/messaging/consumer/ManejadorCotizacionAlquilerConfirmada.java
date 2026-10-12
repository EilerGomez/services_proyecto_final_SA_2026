package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import org.springframework.stereotype.Component;

/*
 * Envía el precio confirmado del alquiler y cómo pagarlo (de Trips).
 */
@Component
public class ManejadorCotizacionAlquilerConfirmada implements ManejadorEvento<CotizacionAlquilerConfirmada> {

    private final CorreoVentasService correos;

    public ManejadorCotizacionAlquilerConfirmada(CorreoVentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "CotizacionAlquilerConfirmada";
    }

    @Override
    public Class<CotizacionAlquilerConfirmada> tipoPayload() {
        return CotizacionAlquilerConfirmada.class;
    }

    @Override
    public void manejar(EventoRecibido<CotizacionAlquilerConfirmada> evento) {
        correos.cotizacionAlquiler(evento.eventId(), evento.payload());
    }
}