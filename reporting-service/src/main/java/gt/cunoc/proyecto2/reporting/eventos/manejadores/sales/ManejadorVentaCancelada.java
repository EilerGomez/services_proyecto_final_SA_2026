package gt.cunoc.proyecto2.reporting.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorVentaCancelada implements ManejadorEvento<VentaCancelada> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorVentaCancelada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VENTA_CANCELADA.nombre();
    }

    @Override
    public Class<VentaCancelada> tipoPayload() {
        return VentaCancelada.class;
    }

    @Override
    public void manejar(EventoRecibido<VentaCancelada> evento) {
        proyeccion.aplicarVentaCancelada(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
