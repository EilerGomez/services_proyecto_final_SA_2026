package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorViajeIniciado implements ManejadorEvento<ViajeIniciado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorViajeIniciado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_INICIADO.nombre();
    }

    @Override
    public Class<ViajeIniciado> tipoPayload() {
        return ViajeIniciado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeIniciado> evento) {
        if (evento.payload().viajeId() != null) {
            proyeccion.marcarViajeEnRuta(
                    evento.payload().viajeId(), evento.payload().busId(), evento.aggregateVersion());
        }
    }
}
