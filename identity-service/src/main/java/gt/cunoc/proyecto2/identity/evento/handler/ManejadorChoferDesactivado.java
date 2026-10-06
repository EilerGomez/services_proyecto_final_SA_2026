package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferDesactivado;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;


/**
 * Baja de chofer: limpia la sucursal si coincide, quita CHOFER y revoca refrescos.
 *
 * <p>{@code chofer_id} se conserva como historial; la cuenta sigue activa como CLIENTE.
 */
@Component
public class ManejadorChoferDesactivado implements ManejadorEvento<ChoferDesactivado> {

    private final UsuarioRepository usuarios;

    private final TokenRefrescoRepository refrescos;

    public ManejadorChoferDesactivado(UsuarioRepository usuarios, TokenRefrescoRepository refrescos) {
        this.usuarios = usuarios;
        this.refrescos = refrescos;
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
        if (usuario == null) {
            return;
        }
        if (desactivado.sucursalId().equals(usuario.getSucursalId())) {
            usuario.setSucursalId(null);
        }
        usuario.getRoles().removeIf(rol -> Roles.CHOFER.equals(rol.getCodigo()));
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }
}
