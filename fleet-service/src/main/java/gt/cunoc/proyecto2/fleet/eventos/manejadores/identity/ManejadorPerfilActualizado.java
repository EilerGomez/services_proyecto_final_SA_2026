package gt.cunoc.proyecto2.fleet.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.fleet.eventos.TipoEvento;
import gt.cunoc.proyecto2.fleet.eventos.recibidos.identity.PerfilActualizado;
import gt.cunoc.proyecto2.fleet.servicios.choferes.ChoferServicio;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import org.springframework.stereotype.Component;

/**
 * PerfilActualizado (Identity): refresca las copias de nombre y correo.
 * 1. Chofer: nombre; si cambió publica ChoferActualizado.
 * 2. Personal de sucursal: nombre y correo en todas sus filas.
 * comun lo ejecuta en una transacción y una sola vez por evento.
 */
@Component
public class ManejadorPerfilActualizado implements ManejadorEvento<PerfilActualizado> {

    private final ChoferServicio choferes;
    private final PersonalSucursalServicio personal;

    public ManejadorPerfilActualizado(ChoferServicio choferes, PersonalSucursalServicio personal) {
        this.choferes = choferes;
        this.personal = personal;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.PERFIL_ACTUALIZADO.nombre();
    }

    @Override
    public Class<PerfilActualizado> tipoPayload() {
        return PerfilActualizado.class;
    }

    @Override
    public void manejar(EventoRecibido<PerfilActualizado> evento) {
        PerfilActualizado perfil = evento.payload();
        String nombreCompleto = perfil.nombreCompleto();

        if (perfil.choferId() != null) {
            choferes.actualizarNombreDesdePerfil(perfil.choferId(), nombreCompleto, evento.businessDate());
        }
        personal.actualizarDatosDePerfil(perfil.usuarioId(), nombreCompleto, perfil.correo());
    }
}