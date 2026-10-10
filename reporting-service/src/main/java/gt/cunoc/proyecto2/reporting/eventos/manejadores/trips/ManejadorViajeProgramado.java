package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorViajeProgramado implements ManejadorEvento<ViajeEvento> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorViajeProgramado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_PROGRAMADO.nombre();
    }

    @Override
    public Class<ViajeEvento> tipoPayload() {
        return ViajeEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeEvento> evento) {
        proyeccion.aplicarViaje(evento.payload(), evento.aggregateVersion());
    }
}
