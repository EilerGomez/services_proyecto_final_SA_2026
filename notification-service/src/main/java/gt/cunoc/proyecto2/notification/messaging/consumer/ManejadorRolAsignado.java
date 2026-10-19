package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.RolAsignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoCuentasService;
import org.springframework.stereotype.Component;

/**
 * Avisa por correo que la cuenta recibió un rol administrativo y que vuelva a iniciar sesión.
 */
@Component
public class ManejadorRolAsignado implements ManejadorEvento<RolAsignado> {

    private final CorreoCuentasService correos;

    public ManejadorRolAsignado(CorreoCuentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "RolAsignado";
    }

    @Override
    public Class<RolAsignado> tipoPayload() {
        return RolAsignado.class;
    }

    @Override
    public void manejar(EventoRecibido<RolAsignado> evento) {
        correos.rolAsignado(evento.eventId(), evento.payload());
    }
}
