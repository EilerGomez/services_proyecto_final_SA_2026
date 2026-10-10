package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferDesactivado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorChoferDesactivado implements ManejadorEvento<ChoferDesactivado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorChoferDesactivado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_DESACTIVADO.nombre();
    }

    @Override
    public Class<ChoferDesactivado> tipoPayload() {
        return ChoferDesactivado.class;
    }

    @Override
    public void manejar(EventoRecibido<ChoferDesactivado> evento) {
        if (evento.payload().choferId() != null) {
            proyeccion.desactivarChofer(evento.payload().choferId(), evento.aggregateVersion());
        }
    }
}
