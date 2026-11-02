package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEliminada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Elimina de la proyección una ruta que Trips dio de baja.
 */
@Component
public class ManejadorRutaEliminada implements ManejadorEvento<RutaEliminada> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorRutaEliminada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.RUTA_ELIMINADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<RutaEliminada> tipoPayload() {
        return RutaEliminada.class;
    }

    // Borra la ruta solo si el evento trae su identificador.
    @Override
    public void manejar(EventoRecibido<RutaEliminada> evento) {
        if (evento.payload().rutaId() != null) {
            proyeccion.eliminarRuta(evento.payload().rutaId());
        }
    }
}
