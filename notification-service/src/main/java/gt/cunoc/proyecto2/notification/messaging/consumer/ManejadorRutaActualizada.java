package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.RutaActualizada;
import gt.cunoc.proyecto2.notification.services.correo.CorreoRutasService;
import org.springframework.stereotype.Component;

/*
 * Avisa a los administradores de las sucursales cuando Trips modifica una ruta.
 */
@Component
public class ManejadorRutaActualizada implements ManejadorEvento<RutaActualizada> {

    private final CorreoRutasService correos;

    public ManejadorRutaActualizada(CorreoRutasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "RutaActualizada";
    }

    @Override
    public Class<RutaActualizada> tipoPayload() {
        return RutaActualizada.class;
    }

    @Override
    public void manejar(EventoRecibido<RutaActualizada> evento) {
        correos.rutaActualizada(evento.eventId(), evento.payload());
    }
}
