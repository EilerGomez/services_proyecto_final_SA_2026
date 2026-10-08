package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.RecuperacionContrasenaSolicitada;
import gt.cunoc.proyecto2.notification.services.correo.EnvioCorreoService;
import org.springframework.stereotype.Component;

/**
 * Envía el enlace de recuperación por correo.
 */
@Component
public class ManejadorRecuperacionContrasena implements ManejadorEvento<RecuperacionContrasenaSolicitada> {

    private final EnvioCorreoService correos;

    public ManejadorRecuperacionContrasena(EnvioCorreoService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "RecuperacionContrasenaSolicitada";
    }

    @Override
    public Class<RecuperacionContrasenaSolicitada> tipoPayload() {
        return RecuperacionContrasenaSolicitada.class;
    }

    @Override
    public void manejar(EventoRecibido<RecuperacionContrasenaSolicitada> evento) {
        RecuperacionContrasenaSolicitada solicitud = evento.payload();
        correos.enviarRecuperacion(
                evento.eventId(), solicitud.correo(), solicitud.nombre(),
                solicitud.urlRecuperacion(), solicitud.expiraEn());
    }
}
