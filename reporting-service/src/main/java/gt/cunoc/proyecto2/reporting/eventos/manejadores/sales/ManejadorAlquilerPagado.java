package gt.cunoc.proyecto2.reporting.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Registra como ingreso el pago de un alquiler privado.
 */
@Component
public class ManejadorAlquilerPagado implements ManejadorEvento<AlquilerPagado> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorAlquilerPagado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.ALQUILER_PAGADO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<AlquilerPagado> tipoPayload() {
        return AlquilerPagado.class;
    }

    // Guarda el ingreso con su monto, su sucursal y el rango de fechas del alquiler.
    @Override
    public void manejar(EventoRecibido<AlquilerPagado> evento) {
        proyeccion.aplicarAlquilerPagado(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
