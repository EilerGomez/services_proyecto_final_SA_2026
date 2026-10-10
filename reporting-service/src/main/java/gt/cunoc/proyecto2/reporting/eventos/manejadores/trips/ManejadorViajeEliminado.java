package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEliminado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorViajeEliminado implements ManejadorEvento<ViajeEliminado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorViajeEliminado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_ELIMINADO.nombre();
    }

    @Override
    public Class<ViajeEliminado> tipoPayload() {
        return ViajeEliminado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeEliminado> evento) {
        if (evento.payload().viajeId() != null) {
            proyeccion.eliminarViaje(evento.payload().viajeId());
        }
    }
}
