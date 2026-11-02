package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Aplica a la proyección una ruta recién creada en Trips.
 */
@Component
public class ManejadorRutaCreada implements ManejadorEvento<RutaEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorRutaCreada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.RUTA_CREADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<RutaEvento> tipoPayload() {
        return RutaEvento.class;
    }

    // Guarda la ruta con sus sucursales, distancia y precio del boleto.
    @Override
    public void manejar(EventoRecibido<RutaEvento> evento) {
        proyeccion.aplicarRuta(evento.payload());
    }
}
