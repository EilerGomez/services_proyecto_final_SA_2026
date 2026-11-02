package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Actualiza en la proyección los datos de un chofer que cambió en Fleet.
 */
@Component
public class ManejadorChoferActualizado implements ManejadorEvento<ChoferEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorChoferActualizado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_ACTUALIZADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ChoferEvento> tipoPayload() {
        return ChoferEvento.class;
    }

    // Aplica los datos nuevos del chofer, incluida su licencia y su estado.
    @Override
    public void manejar(EventoRecibido<ChoferEvento> evento) {
        proyeccion.aplicarChofer(evento.payload(), evento.aggregateVersion());
    }
}
