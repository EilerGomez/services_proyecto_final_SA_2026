package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/*
 * ViajeActualizado (Trips): refresca el viaje (salida, bus, precio) y crea los asientos que falten si subió
 * la capacidad.
 */
@Component
public class ManejadorViajeActualizado implements ManejadorEvento<ViajeEvento> {

    private final ProyeccionServicio proyecciones;

    public ManejadorViajeActualizado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_ACTUALIZADO.nombre();
    }

    @Override
    public Class<ViajeEvento> tipoPayload() {
        return ViajeEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeEvento> evento) {
        if (evento.payload().esRegular()) {
            proyecciones.guardarViaje(evento.payload(), evento.aggregateVersion());
        }
    }
}
