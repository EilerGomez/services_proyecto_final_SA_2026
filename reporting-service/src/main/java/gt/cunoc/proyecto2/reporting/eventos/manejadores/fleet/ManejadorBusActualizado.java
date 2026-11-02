package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Actualiza en la proyección los datos de un bus que cambió en Fleet.
 */
@Component
public class ManejadorBusActualizado implements ManejadorEvento<BusEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorBusActualizado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.BUS_ACTUALIZADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<BusEvento> tipoPayload() {
        return BusEvento.class;
    }

    // Aplica los datos nuevos del bus; la proyección ignora el evento si su versión es anterior.
    @Override
    public void manejar(EventoRecibido<BusEvento> evento) {
        proyeccion.aplicarBus(evento.payload(), evento.aggregateVersion());
    }
}
