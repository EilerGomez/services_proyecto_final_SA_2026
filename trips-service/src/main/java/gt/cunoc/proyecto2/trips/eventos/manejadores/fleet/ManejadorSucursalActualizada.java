package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** SucursalActualizada (Fleet): refresca nombre, ubicación y si está activa. */
@Component
public class ManejadorSucursalActualizada implements ManejadorEvento<SucursalEvento> {

    private final ProyeccionServicio proyecciones;

    public ManejadorSucursalActualizada(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.SUCURSAL_ACTUALIZADA.nombre();
    }

    @Override
    public Class<SucursalEvento> tipoPayload() {
        return SucursalEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<SucursalEvento> evento) {
        proyecciones.guardarSucursal(evento.payload(), evento.aggregateVersion());
    }
}