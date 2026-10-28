package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.AlquilerCancelado;
import gt.cunoc.proyecto2.sales.servicios.alquileres.OrdenAlquilerServicio;
import org.springframework.stereotype.Component;

/**
 * AlquilerCancelado (Trips): la orden de pago pasa a CANCELADA y, si ya había dinero cobrado, se
 * aplica la tarifa de reembolso. Con cartera se le pide a Wallet el porcentaje que toca; en efectivo
 * la orden queda esperando a que la ventanilla entregue el dinero.
 */
@Component
public class ManejadorAlquilerCancelado implements ManejadorEvento<AlquilerCancelado> {

    private final OrdenAlquilerServicio ordenes;

    public ManejadorAlquilerCancelado(OrdenAlquilerServicio ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.ALQUILER_CANCELADO.nombre();
    }

    @Override
    public Class<AlquilerCancelado> tipoPayload() {
        return AlquilerCancelado.class;
    }

    @Override
    public void manejar(EventoRecibido<AlquilerCancelado> evento) {
        ordenes.registrarCancelacion(evento.payload(), evento.businessDate());
    }
}
