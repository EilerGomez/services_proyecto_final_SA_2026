package gt.cunoc.proyecto2.reporting.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Registra como ingreso cada venta de boleto confirmada en Sales.
 */
@Component
public class ManejadorBoletoVendido implements ManejadorEvento<BoletoVendido> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorBoletoVendido(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.BOLETO_VENDIDO.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<BoletoVendido> tipoPayload() {
        return BoletoVendido.class;
    }

    // Guarda el ingreso por boletos con ruta, bus y cliente, sin duplicar el mismo evento.
    @Override
    public void manejar(EventoRecibido<BoletoVendido> evento) {
        proyeccion.aplicarBoletoVendido(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
