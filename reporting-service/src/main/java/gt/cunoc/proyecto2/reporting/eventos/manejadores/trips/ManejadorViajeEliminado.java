package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEliminado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Elimina de la proyección un viaje que Trips dio de baja.
 */
@Component
public class ManejadorViajeEliminado implements ManejadorEvento<ViajeEliminado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorViajeEliminado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_ELIMINADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ViajeEliminado> tipoPayload() {
        return ViajeEliminado.class;
    }

    // Borra el viaje y recalcula el chofer actual del bus que tenía asignado.
    @Override
    public void manejar(EventoRecibido<ViajeEliminado> evento) {
        if (evento.payload().viajeId() != null) {
            proyeccion.eliminarViaje(evento.payload().viajeId());
        }
    }
}
