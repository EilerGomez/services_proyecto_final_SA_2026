package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** BusDesactivado (Fleet): el bus queda INACTIVO en vista_buses. */
@Component
public class ManejadorBusDesactivado implements ManejadorEvento<BusDesactivado> {

    private final ProyeccionServicio proyecciones;

    public ManejadorBusDesactivado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.BUS_DESACTIVADO.nombre();
    }

    @Override
    public Class<BusDesactivado> tipoPayload() {
        return BusDesactivado.class;
    }

    @Override
    public void manejar(EventoRecibido<BusDesactivado> evento) {
        proyecciones.desactivarBus(evento.payload().busId(), evento.aggregateVersion());
    }
}