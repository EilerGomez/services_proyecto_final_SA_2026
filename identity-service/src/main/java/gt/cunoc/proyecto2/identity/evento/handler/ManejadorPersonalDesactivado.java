package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;


/**
 * Baja: limpia la sucursal solo si sigue siendo la del evento, quita el rol y revoca refrescos.
 */
@Component
public class ManejadorPersonalDesactivado implements ManejadorEvento<PersonalSucursalDesactivado> {

    private final UsuarioRepository usuarios;

    private final TokenRefrescoRepository refrescos;

    public ManejadorPersonalDesactivado(UsuarioRepository usuarios, TokenRefrescoRepository refrescos) {
        this.usuarios = usuarios;
        this.refrescos = refrescos;
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
        if (usuario == null || !baja.sucursalId().equals(usuario.getSucursalId())) {
            return;
        }
        usuario.setSucursalId(null);
        usuario.getRoles().removeIf(rol -> baja.rolCodigo().equals(rol.getCodigo()));
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }
}
