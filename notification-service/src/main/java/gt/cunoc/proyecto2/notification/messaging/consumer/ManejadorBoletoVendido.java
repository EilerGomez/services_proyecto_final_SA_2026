package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoVendido;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import org.springframework.stereotype.Component;

/*
 * Envía el comprobante de la compra (de Sales). Sin correo (mostrador) no se envía nada.
 */
@Component
public class ManejadorBoletoVendido implements ManejadorEvento<BoletoVendido> {

    private final CorreoVentasService correos;

    public ManejadorBoletoVendido(CorreoVentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "BoletoVendido";
    }

    @Override
    public Class<BoletoVendido> tipoPayload() {
        return BoletoVendido.class;
    }

    @Override
    public void manejar(EventoRecibido<BoletoVendido> evento) {
        correos.compraBoleto(evento.eventId(), evento.payload());
    }
}