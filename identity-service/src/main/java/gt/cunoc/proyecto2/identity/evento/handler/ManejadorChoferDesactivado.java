package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferDesactivado;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.stereotype.Component;


/**
 * Desactiva la cuenta del chofer: limpia la sucursal y quita CHOFER.
 */
@Component
public class ManejadorChoferDesactivado implements ManejadorEvento<ChoferDesactivado> {

    private final UsuarioRepository usuarios;

    public ManejadorChoferDesactivado(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
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
        ChoferDesactivado desactivado = evento.payload();
        Usuario usuario = usuarios.findByChoferId(desactivado.choferId()).orElse(null);
        if (usuario == null || !"ACTIVO".equals(usuario.getEstado())) {
            return;
        }
        usuario.setEstado("INACTIVO");
        usuario.setSucursalId(null);
        usuario.getRoles().removeIf(rol -> Roles.CHOFER.equals(rol.getCodigo()));
        usuarios.save(usuario);
    }
}
