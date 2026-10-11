package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEliminado;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import org.springframework.stereotype.Component;

/*
 * ViajeEliminado (Trips), compensación: rechaza o cancela las ventas del viaje (con reembolso si fueron con
 * cartera) y luego borra el viaje de vista_viajes. Todo en la misma transacción.
 */
@Component
public class ManejadorViajeEliminado implements ManejadorEvento<ViajeEliminado> {

    private final VentaServicio ventas;
    private final ProyeccionServicio proyecciones;

    public ManejadorViajeEliminado(VentaServicio ventas, ProyeccionServicio proyecciones) {
        this.ventas = ventas;
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_ELIMINADO.nombre();
    }

    @Override
    public Class<ViajeEliminado> tipoPayload() {
        return ViajeEliminado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeEliminado> evento) {
        ViajeEliminado viaje = evento.payload();
        if (!viaje.esRegular()) {
            return;
        }
        ventas.compensarViajeEliminado(viaje.viajeId(), viaje.motivo());     // primero: usa los datos del viaje
        proyecciones.eliminarViaje(viaje.viajeId(), evento.aggregateVersion());
    }
}
