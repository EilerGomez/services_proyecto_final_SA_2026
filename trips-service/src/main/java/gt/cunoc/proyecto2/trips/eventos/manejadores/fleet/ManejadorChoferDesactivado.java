package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferDesactivado;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** ChoferDesactivado (Fleet): el chofer queda INACTIVO en vista_choferes. */
@Component
public class ManejadorChoferDesactivado implements ManejadorEvento<ChoferDesactivado> {

    private final ProyeccionServicio proyecciones;

    public ManejadorChoferDesactivado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_DESACTIVADO.nombre();
    }

    @Override
    public Class<ChoferDesactivado> tipoPayload() {
        return ChoferDesactivado.class;
    }

    @Override
    public void manejar(EventoRecibido<ChoferDesactivado> evento) {
        proyecciones.desactivarChofer(evento.payload().choferId(), evento.aggregateVersion());
    }
}