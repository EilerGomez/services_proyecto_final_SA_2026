package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.sales.servicios.alquileres.OrdenAlquilerServicio;
import org.springframework.stereotype.Component;

/* CotizacionAlquilerConfirmada (Trips): crea la orden de pago del alquiler (S9–S13). */
@Component
public class ManejadorCotizacionAlquilerConfirmada implements ManejadorEvento<CotizacionAlquilerConfirmada> {

    private final OrdenAlquilerServicio ordenes;

    public ManejadorCotizacionAlquilerConfirmada(OrdenAlquilerServicio ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.COTIZACION_ALQUILER_CONFIRMADA.nombre();
    }

    @Override
    public Class<CotizacionAlquilerConfirmada> tipoPayload() {
        return CotizacionAlquilerConfirmada.class;
    }

    @Override
    public void manejar(EventoRecibido<CotizacionAlquilerConfirmada> evento) {
        ordenes.registrarOrden(evento.payload());
    }
}
