package gt.cunoc.proyecto2.fleet.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.fleet.eventos.TipoEvento;
import gt.cunoc.proyecto2.fleet.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.fleet.servicios.bitacora.BitacoraKilometrajeServicio;
import org.springframework.stereotype.Component;

/** ViajeIniciado (Trips): el bus pasa a EN_RUTA y se publica BusActualizado. */
@Component
public class ManejadorViajeIniciado implements ManejadorEvento<ViajeIniciado> {

    private final BitacoraKilometrajeServicio viajes;

    public ManejadorViajeIniciado(BitacoraKilometrajeServicio viajes) {
        this.viajes = viajes;
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
        ViajeIniciado viaje = evento.payload();
        viajes.registrarViajeIniciado(viaje.busId(), viaje.viajeId(), evento.businessDate());
    }
}