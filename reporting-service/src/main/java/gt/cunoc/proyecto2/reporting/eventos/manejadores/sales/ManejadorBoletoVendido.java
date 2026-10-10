package gt.cunoc.proyecto2.reporting.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorBoletoVendido implements ManejadorEvento<BoletoVendido> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorBoletoVendido(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.BOLETO_VENDIDO.nombre();
    }

    @Override
    public Class<BoletoVendido> tipoPayload() {
        return BoletoVendido.class;
    }

    @Override
    public void manejar(EventoRecibido<BoletoVendido> evento) {
        proyeccion.aplicarBoletoVendido(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
