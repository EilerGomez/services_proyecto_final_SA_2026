package gt.cunoc.proyecto2.reporting.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Marca el viaje como EN_RUTA cuando Trips avisa que arrancó.
 */
@Component
public class ManejadorViajeIniciado implements ManejadorEvento<ViajeIniciado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorViajeIniciado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_INICIADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<ViajeIniciado> tipoPayload() {
        return ViajeIniciado.class;
    }

    // Cambia el estado y actualiza el chofer que va al frente del bus.
    @Override
    public void manejar(EventoRecibido<ViajeIniciado> evento) {
        if (evento.payload().viajeId() != null) {
            proyeccion.marcarViajeEnRuta(
                    evento.payload().viajeId(), evento.payload().busId(), evento.aggregateVersion());
        }
    }
}
