package gt.cunoc.proyecto2.notification.messaging.consumer;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.CodigoDobleFactorSolicitado;
import gt.cunoc.proyecto2.notification.services.correo.EnvioCorreoService;
import org.springframework.stereotype.Component;


/**
 * Envía el código de doble factor por correo.
 */
@Component
public class ManejadorCodigoDobleFactor implements ManejadorEvento<CodigoDobleFactorSolicitado> {

    private final EnvioCorreoService correos;

    public ManejadorCodigoDobleFactor(EnvioCorreoService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "CodigoDobleFactorSolicitado";
    }

    @Override
    public Class<CodigoDobleFactorSolicitado> tipoPayload() {
        return CodigoDobleFactorSolicitado.class;
    }

    @Override
    public void manejar(EventoRecibido<CodigoDobleFactorSolicitado> evento) {
        CodigoDobleFactorSolicitado solicitud = evento.payload();
        correos.enviarCodigoDobleFactor(
                evento.eventId(), solicitud.correo(), solicitud.nombre(),
                solicitud.codigo(), solicitud.expiraEn());
    }
}
