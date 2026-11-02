package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Aplica a la proyección un bus recién creado en Fleet.
 */
@Component
public class ManejadorBusCreado implements ManejadorEvento<BusEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorBusCreado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.BUS_CREADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<BusEvento> tipoPayload() {
        return BusEvento.class;
    }

    // Inserta el bus con su versión; si la versión es vieja, la proyección la descarta.
    @Override
    public void manejar(EventoRecibido<BusEvento> evento) {
        proyeccion.aplicarBus(evento.payload(), evento.aggregateVersion());
    }
}
