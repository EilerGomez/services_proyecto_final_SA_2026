package gt.cunoc.proyecto2.identity.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalAsignado;
import gt.cunoc.proyecto2.identity.services.personal.SincronizacionPersonalService;
import org.springframework.stereotype.Component;

@Component
public class ManejadorPersonalAsignado implements ManejadorEvento<PersonalSucursalAsignado> {

    private final SincronizacionPersonalService servicio;

    public ManejadorPersonalAsignado(SincronizacionPersonalService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "PersonalSucursalAsignado";
    }

    @Override
    public Class<PersonalSucursalAsignado> tipoPayload() {
        return PersonalSucursalAsignado.class;
    }

    @Override
    public void manejar(EventoRecibido<PersonalSucursalAsignado> evento) {
        PersonalSucursalAsignado payload = evento.payload();
        servicio.asignarPersonal(payload.usuarioId(), payload.sucursalId(), payload.rolCodigo());
    }
}
