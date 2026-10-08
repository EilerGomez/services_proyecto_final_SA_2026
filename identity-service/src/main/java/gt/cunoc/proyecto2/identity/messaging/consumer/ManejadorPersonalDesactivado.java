package gt.cunoc.proyecto2.identity.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.identity.services.personal.SincronizacionPersonalService;
import org.springframework.stereotype.Component;

@Component
public class ManejadorPersonalDesactivado implements ManejadorEvento<PersonalSucursalDesactivado> {

    private final SincronizacionPersonalService servicio;

    public ManejadorPersonalDesactivado(SincronizacionPersonalService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "PersonalSucursalDesactivado";
    }

    @Override
    public Class<PersonalSucursalDesactivado> tipoPayload() {
        return PersonalSucursalDesactivado.class;
    }

    @Override
    public void manejar(EventoRecibido<PersonalSucursalDesactivado> evento) {
        PersonalSucursalDesactivado payload = evento.payload();
        servicio.desactivarPersonal(payload.usuarioId(), payload.sucursalId(), payload.rolCodigo());
    }
}
