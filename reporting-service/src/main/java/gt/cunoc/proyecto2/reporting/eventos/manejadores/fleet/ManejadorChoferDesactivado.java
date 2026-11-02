package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferDesactivado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Marca como inactivo en la proyección un chofer desactivado en Fleet.
 */
@Component
public class ManejadorChoferDesactivado implements ManejadorEvento<ChoferDesactivado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorChoferDesactivado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_DESACTIVADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ChoferDesactivado> tipoPayload() {
        return ChoferDesactivado.class;
    }

    // Desactiva el chofer si el evento trae su identificador.
    @Override
    public void manejar(EventoRecibido<ChoferDesactivado> evento) {
        if (evento.payload().choferId() != null) {
            proyeccion.desactivarChofer(evento.payload().choferId(), evento.aggregateVersion());
        }
    }
}
