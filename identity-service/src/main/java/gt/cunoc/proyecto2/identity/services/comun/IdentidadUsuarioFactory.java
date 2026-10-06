package gt.cunoc.proyecto2.identity.services.comun;


import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.models.Usuario;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Construye el {@link UsuarioActual} (claims del JWT) desde un {@link Usuario}.
 *
 * <p>La sucursal vive en {@code usuarios.sucursal_id} (la mantiene Fleet vía R5 y eventos).
 * Se refleja en el JWT en el siguiente inicio de sesión.
 */
@Component
public class IdentidadUsuarioFactory {

    public UsuarioActual construir(Usuario usuario) {
        Set<String> roles = usuario.getRoles().stream()
                .map(rol -> rol.getCodigo())
                .collect(Collectors.toSet());
        return new UsuarioActual(
                usuario.getId(),
                usuario.getCorreo(),
                roles,
                sucursalDe(usuario, roles),
                usuario.getChoferId());
    }

    private UUID sucursalDe(Usuario usuario, Set<String> roles) {
        if (!roles.contains("ADMIN_SUCURSAL") && !roles.contains("CAJERO") && !roles.contains("CHOFER")) {
            return null;
        }
        return usuario.getSucursalId();
    }
}
