package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** ChoferCreado (Fleet): agrega el chofer a vista_choferes. */
@Component
public class ManejadorChoferCreado implements ManejadorEvento<ChoferEvento> {

    private final ProyeccionServicio proyecciones;

    public ManejadorChoferCreado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_CREADO.nombre();
    }

    @Override
    public Class<ChoferEvento> tipoPayload() {
        return ChoferEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<ChoferEvento> evento) {
        proyecciones.guardarChofer(evento.payload(), evento.aggregateVersion());
    }
}