package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/* ViajeIniciado (Trips): el viaje queda EN_RUTA; S1, S2 y S4 ya no lo venden. */
@Component
public class ManejadorViajeIniciado implements ManejadorEvento<ViajeIniciado> {

    private final ProyeccionServicio proyecciones;

    public ManejadorViajeIniciado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_INICIADO.nombre();
    }

    @Override
    public Class<ViajeIniciado> tipoPayload() {
        return ViajeIniciado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeIniciado> evento) {
        if (evento.payload().esRegular()) {
            proyecciones.iniciarViaje(evento.payload().viajeId(), evento.aggregateVersion());
        }
    }
}
