package gt.cunoc.proyecto2.reporting.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorTarifaDepreciacionCambiada implements ManejadorEvento<TarifaDepreciacionCambiada> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorTarifaDepreciacionCambiada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.TARIFA_DEPRECIACION_CAMBIADA.nombre();
    }

    @Override
    public Class<TarifaDepreciacionCambiada> tipoPayload() {
        return TarifaDepreciacionCambiada.class;
    }

    @Override
    public void manejar(EventoRecibido<TarifaDepreciacionCambiada> evento) {
        proyeccion.aplicarTarifa(evento.payload());
    }
}
