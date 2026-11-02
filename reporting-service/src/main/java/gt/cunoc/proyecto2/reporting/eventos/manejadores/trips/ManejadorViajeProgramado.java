package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Aplica a la proyección un viaje recién programado en Trips.
 */
@Component
public class ManejadorViajeProgramado implements ManejadorEvento<ViajeEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorViajeProgramado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_PROGRAMADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ViajeEvento> tipoPayload() {
        return ViajeEvento.class;
    }

    // Crea el viaje con su ruta, bus, chofer y fecha de salida.
    @Override
    public void manejar(EventoRecibido<ViajeEvento> evento) {
        proyeccion.aplicarViaje(evento.payload(), evento.aggregateVersion());
    }
}
