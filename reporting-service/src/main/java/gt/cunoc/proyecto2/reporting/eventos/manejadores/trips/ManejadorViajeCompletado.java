package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Cierra el viaje y genera los gastos de combustible, depreciación y salario del chofer.
 */
@Component
public class ManejadorViajeCompletado implements ManejadorEvento<ViajeCompletado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorViajeCompletado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_COMPLETADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ViajeCompletado> tipoPayload() {
        return ViajeCompletado.class;
    }

    // Aplica el cierre con los kilómetros y costos del viaje, y actualiza los contadores.
    @Override
    public void manejar(EventoRecibido<ViajeCompletado> evento) {
        proyeccion.aplicarViajeCompletado(
                evento.eventId(), evento.payload(), evento.aggregateVersion(), evento.businessDate());
    }
}
