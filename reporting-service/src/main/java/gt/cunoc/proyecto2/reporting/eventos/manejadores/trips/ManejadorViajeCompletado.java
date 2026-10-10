package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * ViajeCompletado: cierra el viaje y genera 3 gastos
 * (combustible, depreciación y salario del chofer).
 */
@Component
public class ManejadorViajeCompletado implements ManejadorEvento<ViajeCompletado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorViajeCompletado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_COMPLETADO.nombre();
    }

    @Override
    public Class<ViajeCompletado> tipoPayload() {
        return ViajeCompletado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeCompletado> evento) {
        proyeccion.aplicarViajeCompletado(
                evento.eventId(), evento.payload(), evento.aggregateVersion(), evento.businessDate());
    }
}
