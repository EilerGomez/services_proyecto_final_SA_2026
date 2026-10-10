package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** ChoferActualizado (Fleet): refresca nombre, salario base, sucursal y estado. */
@Component
public class ManejadorChoferActualizado implements ManejadorEvento<ChoferEvento> {

    private final ProyeccionServicio proyecciones;

    public ManejadorChoferActualizado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CHOFER_ACTUALIZADO.nombre();
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