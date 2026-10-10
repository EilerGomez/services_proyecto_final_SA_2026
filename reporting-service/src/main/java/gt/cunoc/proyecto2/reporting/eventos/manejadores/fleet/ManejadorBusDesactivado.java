package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorBusDesactivado implements ManejadorEvento<BusDesactivado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorBusDesactivado(ProyeccionReportingServicio proyeccion) {
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
        if (evento.payload().busId() != null) {
            proyeccion.desactivarBus(evento.payload().busId(), evento.aggregateVersion());
        }
    }
}
