package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.stereotype.Component;


/**
 * Refleja la baja: limpia la sucursal y quita ADMIN_SUCURSAL/CAJERO.
 */
@Component
public class ManejadorPersonalDesactivado implements ManejadorEvento<PersonalSucursalDesactivado> {

    private final UsuarioRepository usuarios;

    public ManejadorPersonalDesactivado(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
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
        PersonalSucursalDesactivado baja = evento.payload();
        Usuario usuario = usuarios.findById(baja.usuarioId()).orElse(null);
        if (usuario == null) {
            return;
        }
        usuario.setSucursalId(null);
        usuario.getRoles().removeIf(rol ->
                Roles.ADMIN_SUCURSAL.equals(rol.getCodigo()) || Roles.CAJERO.equals(rol.getCodigo()));
        usuarios.save(usuario);
    }
}
