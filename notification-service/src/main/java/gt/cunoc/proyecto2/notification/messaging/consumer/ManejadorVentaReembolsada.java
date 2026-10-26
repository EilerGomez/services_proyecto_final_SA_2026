package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.VentaReembolsada;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import org.springframework.stereotype.Component;

/**
 * Avisa que ya se devolvió el dinero de una compra cancelada (de Sales). Si la venta se registró sin
 * correo (mostrador sin datos del cliente) no se envía nada.
 */
@Component
public class ManejadorVentaReembolsada implements ManejadorEvento<VentaReembolsada> {

    private final CorreoVentasService correos;

    public ManejadorVentaReembolsada(CorreoVentasService correos) {
        this.correos = correos;
    }

    @Override
    public String tipoEvento() {
        return "VentaReembolsada";
    }

    @Override
    public Class<VentaReembolsada> tipoPayload() {
        return VentaReembolsada.class;
    }

    @Override
    public void manejar(EventoRecibido<VentaReembolsada> evento) {
        correos.ventaReembolsada(evento.eventId(), evento.payload());
    }
}
