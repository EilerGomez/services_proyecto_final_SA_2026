package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantiene sucursales, roles y sesiones de las cuentas de personal. */
@Service
public class SincronizacionPersonalService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final TokenRefrescoRepository refrescos;

    public SincronizacionPersonalService(UsuarioRepository usuarios, RolRepository roles, TokenRefrescoRepository refrescos) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.refrescos = refrescos;
    }

    @Transactional
    public void desactivarChofer(UUID choferId, UUID sucursalId) {
        Usuario usuario = usuarios.findByChoferId(choferId).orElse(null);

        if (usuario == null || !Objects.equals(sucursalId, usuario.getSucursalId())) {
            return;
        }
        usuario.setSucursalId(null);

        usuario.getRoles().removeIf(rol -> Roles.CHOFER.equals(rol.getCodigo()));
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }

    @Transactional
    public void asignarPersonal(UUID usuarioId, UUID sucursalId, String rolCodigo) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        if (usuario == null) {
            return;
        }

        usuario.setSucursalId(sucursalId);
        Rol rol = roles.findById(rolCodigo).orElse(null);

        if (rol != null && (Roles.ADMIN_SUCURSAL.equals(rol.getCodigo()) || Roles.CAJERO.equals(rol.getCodigo()))) {
            usuario.getRoles().add(rol);
        }
        usuarios.save(usuario);
    }

    @Transactional
    public void desactivarPersonal(UUID usuarioId, UUID sucursalId, String rolCodigo) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        if (usuario == null || !Objects.equals(sucursalId, usuario.getSucursalId())) {
            return;
        }
        usuario.setSucursalId(null);
        usuario.getRoles().removeIf(rol -> rolCodigo.equals(rol.getCodigo()));
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }
}
