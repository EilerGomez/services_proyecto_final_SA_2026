package gt.cunoc.proyecto2.reporting.eventos.manejadores.maintenance;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.GastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Registra como gastos las líneas de un servicio de taller recién capturado.
 */
@Component
public class ManejadorGastoTallerRegistrado implements ManejadorEvento<GastoTallerRegistrado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorGastoTallerRegistrado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.GASTO_TALLER_REGISTRADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<GastoTallerRegistrado> tipoPayload() {
        return GastoTallerRegistrado.class;
    }

    // Convierte cada línea del servicio en un gasto, usando la fecha de negocio del evento.
    @Override
    public void manejar(EventoRecibido<GastoTallerRegistrado> evento) {
        proyeccion.aplicarGastoTaller(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
