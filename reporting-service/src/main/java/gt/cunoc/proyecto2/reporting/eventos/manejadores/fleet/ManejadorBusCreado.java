package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorBusCreado implements ManejadorEvento<BusEvento> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorBusCreado(ProyeccionReportingServicio proyeccion) {
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
        proyeccion.aplicarBus(evento.payload(), evento.aggregateVersion());
    }
}
