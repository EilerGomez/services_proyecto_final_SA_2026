package gt.cunoc.proyecto2.trips.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** TarifaDepreciacionCambiada (Identity): guarda la tarifa nueva; la usa T15 según el día de llegada. */
@Component
public class ManejadorTarifaDepreciacionCambiada implements ManejadorEvento<TarifaDepreciacionCambiada> {

    private final ProyeccionServicio proyecciones;

    public ManejadorTarifaDepreciacionCambiada(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
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
        proyecciones.guardarTarifa(evento.payload());
    }
}