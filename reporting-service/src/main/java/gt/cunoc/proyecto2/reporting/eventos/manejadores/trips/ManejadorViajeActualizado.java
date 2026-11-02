package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Actualiza en la proyección un viaje al que le cambiaron datos o le reasignaron el bus.
 */
@Component
public class ManejadorViajeActualizado implements ManejadorEvento<ViajeEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorViajeActualizado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_ACTUALIZADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ViajeEvento> tipoPayload() {
        return ViajeEvento.class;
    }

    // Aplica el cambio y recalcula el chofer del bus anterior si hubo reasignación.
    @Override
    public void manejar(EventoRecibido<ViajeEvento> evento) {
        proyeccion.aplicarViajeActualizado(evento.payload(), evento.aggregateVersion());
    }
}
