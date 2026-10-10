package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorChoferCreado implements ManejadorEvento<ChoferEvento> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorChoferCreado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_CREADO.nombre();
    }

    @Override
    public Class<ChoferEvento> tipoPayload() {
        return ChoferEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<ChoferEvento> evento) {
        proyeccion.aplicarChofer(evento.payload(), evento.aggregateVersion());
    }
}
