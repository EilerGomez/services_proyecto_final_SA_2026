package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Actualiza en la proyección una ruta que cambió en Trips.
 */
@Component
public class ManejadorRutaActualizada implements ManejadorEvento<RutaEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorRutaActualizada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.RUTA_ACTUALIZADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<RutaEvento> tipoPayload() {
        return RutaEvento.class;
    }

    // Aplica precio, distancia o estado nuevos; los reportes usan estos datos al agrupar.
    @Override
    public void manejar(EventoRecibido<RutaEvento> evento) {
        proyeccion.aplicarRuta(evento.payload());
    }
}
