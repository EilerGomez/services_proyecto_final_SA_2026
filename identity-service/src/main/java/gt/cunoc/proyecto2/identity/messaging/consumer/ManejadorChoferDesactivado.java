package gt.cunoc.proyecto2.identity.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferDesactivado;
import gt.cunoc.proyecto2.identity.services.personal.SincronizacionPersonalService;
import org.springframework.stereotype.Component;

@Component
public class ManejadorChoferDesactivado implements ManejadorEvento<ChoferDesactivado> {

    private final SincronizacionPersonalService servicio;

    public ManejadorChoferDesactivado(SincronizacionPersonalService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "ChoferDesactivado";
    }

    @Override
    public Class<ChoferDesactivado> tipoPayload() {
        return ChoferDesactivado.class;
    }

    @Override
    public void manejar(EventoRecibido<ChoferDesactivado> evento) {
        ChoferDesactivado payload = evento.payload();
        servicio.desactivarChofer(payload.choferId(), payload.sucursalId());
    }
}
