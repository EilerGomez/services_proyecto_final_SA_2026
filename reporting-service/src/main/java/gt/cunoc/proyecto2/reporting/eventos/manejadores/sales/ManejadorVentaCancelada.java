package gt.cunoc.proyecto2.reporting.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Registra el reembolso de una venta cancelada como ingreso negativo.
 */
@Component
public class ManejadorVentaCancelada implements ManejadorEvento<VentaCancelada> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorVentaCancelada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.VENTA_CANCELADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<VentaCancelada> tipoPayload() {
        return VentaCancelada.class;
    }

    // Resta el monto anulado; si la venta original aún no llegó, el servicio pide reintentar.
    @Override
    public void manejar(EventoRecibido<VentaCancelada> evento) {
        proyeccion.aplicarVentaCancelada(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
