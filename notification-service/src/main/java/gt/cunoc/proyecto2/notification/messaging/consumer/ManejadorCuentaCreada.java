package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.CuentaCreada;
import gt.cunoc.proyecto2.notification.services.correo.CorreoCuentasService;
import org.springframework.stereotype.Component;

/**
 * Envía el correo de bienvenida o activación (con el puesto si es personal, chofer o administrador).
 */
@Component
public class ManejadorCuentaCreada implements ManejadorEvento<CuentaCreada> {

    private final CorreoCuentasService correos;

    public ManejadorCuentaCreada(CorreoCuentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "CuentaCreada";
    }

    @Override
    public Class<CuentaCreada> tipoPayload() {
        return CuentaCreada.class;
    }

    @Override
    public void manejar(EventoRecibido<CuentaCreada> evento) {
        correos.bienvenida(evento.eventId(), evento.payload());
    }
}
