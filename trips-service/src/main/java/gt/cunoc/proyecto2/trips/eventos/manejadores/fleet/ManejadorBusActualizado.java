package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** BusActualizado (Fleet): refresca placa, capacidad, sucursal y estado. */
@Component
public class ManejadorBusActualizado implements ManejadorEvento<BusEvento> {

    private final ProyeccionServicio proyecciones;

    public ManejadorBusActualizado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.BUS_ACTUALIZADO.nombre();
    }

    @Override
    public Class<BusEvento> tipoPayload() {
        return BusEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<BusEvento> evento) {
        proyecciones.guardarBus(evento.payload(), evento.aggregateVersion());
    }
}