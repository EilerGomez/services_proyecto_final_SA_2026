package gt.cunoc.proyecto2.fleet.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.fleet.eventos.TipoEvento;
import gt.cunoc.proyecto2.fleet.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.fleet.servicios.bitacora.BitacoraKilometrajeServicio;
import org.springframework.stereotype.Component;

/**
 * ViajeCompletado (Trips): registra el viaje en la bitácora, sube el kilometraje del bus,
 * lo regresa a ACTIVO y publica BusActualizado.
 */
@Component
public class ManejadorViajeCompletado implements ManejadorEvento<ViajeCompletado> {

    private final BitacoraKilometrajeServicio viajes;

    public ManejadorViajeCompletado(BitacoraKilometrajeServicio viajes) {
        this.viajes = viajes;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_COMPLETADO.nombre();
    }

    @Override
    public Class<ViajeCompletado> tipoPayload() {
        return ViajeCompletado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeCompletado> evento) {
        ViajeCompletado viaje = evento.payload();
        viajes.registrarViajeCompletado(viaje.busId(), viaje.viajeId(), viaje.kmInicial(), viaje.kmFinal(),
                viaje.fechaHoraLlegada(), evento.businessDate());
    }
}