package gt.cunoc.proyecto2.identity.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferCreado;
import gt.cunoc.proyecto2.identity.services.personal.SincronizacionPersonalService;
import org.springframework.stereotype.Component;

/** ChoferCreado: si el chofer se dio de alta con una cuenta que ya existía, se le pone el rol CHOFER. */
@Component
public class ManejadorChoferCreado implements ManejadorEvento<ChoferCreado> {

    private final SincronizacionPersonalService servicio;

    public ManejadorChoferCreado(SincronizacionPersonalService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "ChoferCreado";
    }

    @Override
    public Class<ChoferCreado> tipoPayload() {
        return ChoferCreado.class;
    }

    @Override
    public void manejar(EventoRecibido<ChoferCreado> evento) {
        ChoferCreado payload = evento.payload();
        servicio.asignarChofer(payload.usuarioId(), payload.choferId(), payload.sucursalId());
    }
}
