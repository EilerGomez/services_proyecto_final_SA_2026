package gt.cunoc.proyecto2.maintenance.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.maintenance.eventos.TipoEvento;
import gt.cunoc.proyecto2.maintenance.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.maintenance.servicios.buses.ProyeccionBuses;
import org.springframework.stereotype.Component;

@Component
public class ManejadorBusDesactivado implements ManejadorEvento<BusDesactivado> {

    private final ProyeccionBuses proyeccion;

    public ManejadorBusDesactivado(ProyeccionBuses proyeccion) {
        this.proyeccion = proyeccion;
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
        // no se oculta: un evento sin busId se reintenta y termina en la DLQ para revisarlo
        if (evento.payload().busId() == null) {
            throw new IllegalArgumentException("BusDesactivado sin busId (evento " + evento.eventId() + ")");
        }
        proyeccion.desactivar(evento.payload().busId(), evento.aggregateVersion());
    }
}
