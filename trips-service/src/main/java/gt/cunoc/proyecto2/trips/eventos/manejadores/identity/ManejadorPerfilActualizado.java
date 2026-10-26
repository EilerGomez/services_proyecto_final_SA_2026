package gt.cunoc.proyecto2.trips.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.PerfilActualizado;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/** PerfilActualizado (Identity): el cliente cambió su NIT, su nombre o su correo. */
@Component
public class ManejadorPerfilActualizado implements ManejadorEvento<PerfilActualizado> {

    private final ProyeccionServicio proyecciones;

    public ManejadorPerfilActualizado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
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
        proyecciones.guardarCliente(perfil.usuarioId(), perfil.nit(), perfil.nombreCompleto(), perfil.correo());
    }
}
