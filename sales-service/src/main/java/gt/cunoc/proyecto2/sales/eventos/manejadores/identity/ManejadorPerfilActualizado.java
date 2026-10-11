package gt.cunoc.proyecto2.sales.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.identity.PerfilActualizado;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/*PerfilActualizado (Identity): actualiza NIT, nombre y correo en vista_clientes. */
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
