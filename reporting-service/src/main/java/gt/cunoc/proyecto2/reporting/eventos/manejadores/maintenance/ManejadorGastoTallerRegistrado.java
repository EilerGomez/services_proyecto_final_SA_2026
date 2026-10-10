package gt.cunoc.proyecto2.reporting.eventos.manejadores.maintenance;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.GastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorGastoTallerRegistrado implements ManejadorEvento<GastoTallerRegistrado> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorGastoTallerRegistrado(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.GASTO_TALLER_REGISTRADO.nombre();
    }

    @Override
    public Class<GastoTallerRegistrado> tipoPayload() {
        return GastoTallerRegistrado.class;
    }

    @Override
    public void manejar(EventoRecibido<GastoTallerRegistrado> evento) {
        proyeccion.aplicarGastoTaller(evento.eventId(), evento.payload(), evento.businessDate());
    }
}
