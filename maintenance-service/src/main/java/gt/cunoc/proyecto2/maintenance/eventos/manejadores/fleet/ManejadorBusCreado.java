package gt.cunoc.proyecto2.maintenance.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.maintenance.eventos.TipoEvento;
import gt.cunoc.proyecto2.maintenance.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.maintenance.servicios.buses.ProyeccionBuses;
import org.springframework.stereotype.Component;

@Component
public class ManejadorBusCreado implements ManejadorEvento<BusEvento> {

    private final ProyeccionBuses proyeccion;

    public ManejadorBusCreado(ProyeccionBuses proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.BUS_CREADO.nombre();
    }

    @Override
    public Class<BusEvento> tipoPayload() {
        return BusEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<BusEvento> evento) {
        BusEvento bus = evento.payload();
        proyeccion.guardar(bus.busId(), bus.sucursalId(), bus.placa(), bus.estado(),
                evento.aggregateVersion());
    }
}
