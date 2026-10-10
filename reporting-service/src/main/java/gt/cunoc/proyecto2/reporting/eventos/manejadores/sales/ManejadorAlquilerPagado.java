package gt.cunoc.proyecto2.reporting.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorAlquilerPagado implements ManejadorEvento<AlquilerPagado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorAlquilerPagado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
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
        proyeccion.aplicarAlquilerPagado(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
