package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Aplica a la proyección un chofer recién creado en Fleet.
 */
@Component
public class ManejadorChoferCreado implements ManejadorEvento<ChoferEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorChoferCreado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_CREADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ChoferEvento> tipoPayload() {
        return ChoferEvento.class;
    }

    // Inserta el chofer con su versión para que los eventos posteriores se comparen contra ella.
    @Override
    public void manejar(EventoRecibido<ChoferEvento> evento) {
        proyeccion.aplicarChofer(evento.payload(), evento.aggregateVersion());
    }
}
