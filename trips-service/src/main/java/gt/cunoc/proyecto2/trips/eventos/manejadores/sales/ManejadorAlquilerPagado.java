package gt.cunoc.proyecto2.trips.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.trips.servicios.alquileres.AlquilerServicio;
import org.springframework.stereotype.Component;

/** AlquilerPagado (Sales): el alquiler pasa de COTIZADO a PAGADO y ya se le puede asignar bus (T22). */
@Component
public class ManejadorAlquilerPagado implements ManejadorEvento<AlquilerPagado> {

    private final AlquilerServicio alquileres;

    public ManejadorAlquilerPagado(AlquilerServicio alquileres) {
        this.alquileres = alquileres;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.ALQUILER_PAGADO.nombre();
    }

    @Override
    public Class<AlquilerPagado> tipoPayload() {
        return AlquilerPagado.class;
    }

    @Override
    public void manejar(EventoRecibido<AlquilerPagado> evento) {
        alquileres.registrarPago(evento.payload().alquilerId());
    }
}