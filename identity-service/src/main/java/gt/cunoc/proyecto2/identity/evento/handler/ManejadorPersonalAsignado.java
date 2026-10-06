package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalAsignado;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.stereotype.Component;


/**
 * Traslado o alta: fija la sucursal y agrega el rol (idempotente).
 */
@Component
public class ManejadorPersonalAsignado implements ManejadorEvento<PersonalSucursalAsignado> {

    private final UsuarioRepository usuarios;

    private final RolRepository roles;

    public ManejadorPersonalAsignado(UsuarioRepository usuarios, RolRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
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
        PersonalSucursalAsignado asignado = evento.payload();
        Usuario usuario = usuarios.findById(asignado.usuarioId()).orElse(null);
        if (usuario == null) {
            return;
        }
        usuario.setSucursalId(asignado.sucursalId());
        Rol rol = roles.findById(asignado.rolCodigo()).orElse(null);
        if (rol != null && (Roles.ADMIN_SUCURSAL.equals(rol.getCodigo()) || Roles.CAJERO.equals(rol.getCodigo()))) {
            usuario.getRoles().add(rol);
        }
        usuarios.save(usuario);
    }
}
