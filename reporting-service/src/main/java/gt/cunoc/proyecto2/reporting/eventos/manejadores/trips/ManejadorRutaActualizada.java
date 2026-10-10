package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorRutaActualizada implements ManejadorEvento<RutaEvento> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorRutaActualizada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.RUTA_ACTUALIZADA.nombre();
    }

    @Override
    public Class<RutaEvento> tipoPayload() {
        return RutaEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<RutaEvento> evento) {
        proyeccion.aplicarRuta(evento.payload());
    }
}
