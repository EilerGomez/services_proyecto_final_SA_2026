package gt.cunoc.proyecto2.trips.eventos.manejadores.sales;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.sales.TarifaReembolsoCambiada;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/**
 * TarifaReembolsoCambiada (Sales): actualiza la copia local de la política de cancelación, con la que
 * Trips rechaza una cancelación fuera de plazo sin tener que preguntarle nada a Sales.
 */
@Component
public class ManejadorTarifaReembolsoCambiada implements ManejadorEvento<TarifaReembolsoCambiada> {

    private final ProyeccionServicio proyecciones;

    public ManejadorTarifaReembolsoCambiada(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.TARIFA_REEMBOLSO_CAMBIADA.nombre();
    }

    @Override
    public Class<TarifaReembolsoCambiada> tipoPayload() {
        return TarifaReembolsoCambiada.class;
    }

    @Override
    public void manejar(EventoRecibido<TarifaReembolsoCambiada> evento) {
        proyecciones.guardarTarifaReembolso(evento.payload());
    }
}
