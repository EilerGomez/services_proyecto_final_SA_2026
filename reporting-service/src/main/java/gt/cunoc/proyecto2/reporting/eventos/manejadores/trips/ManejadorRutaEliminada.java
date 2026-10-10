package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEliminada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorRutaEliminada implements ManejadorEvento<RutaEliminada> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorRutaEliminada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.RUTA_ELIMINADA.nombre();
    }

    @Override
    public Class<RutaEliminada> tipoPayload() {
        return RutaEliminada.class;
    }

    @Override
    public void manejar(EventoRecibido<RutaEliminada> evento) {
        if (evento.payload().rutaId() != null) {
            proyeccion.eliminarRuta(evento.payload().rutaId());
        }
    }
}
