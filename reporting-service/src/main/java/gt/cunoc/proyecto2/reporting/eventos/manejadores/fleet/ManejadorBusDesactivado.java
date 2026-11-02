package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Marca como inactivo en la proyección un bus desactivado en Fleet.
 */
@Component
public class ManejadorBusDesactivado implements ManejadorEvento<BusDesactivado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorBusDesactivado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.BUS_DESACTIVADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<BusDesactivado> tipoPayload() {
        return BusDesactivado.class;
    }

    // Desactiva el bus si el evento trae su identificador; sin él no hay nada que actualizar.
    @Override
    public void manejar(EventoRecibido<BusDesactivado> evento) {
        if (evento.payload().busId() != null) {
            proyeccion.desactivarBus(evento.payload().busId(), evento.aggregateVersion());
        }
    }
}
