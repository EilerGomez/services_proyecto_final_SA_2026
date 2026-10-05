package gt.cunoc.proyecto2.comun.seguridad;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import java.util.Set;
import java.util.UUID;

/**
 * Usuario autenticado (claims del JWT). Se recibe como parámetro en los controladores:
 * {@code public Bus crear(@RequestBody @Valid BusRequest r, @Parameter(hidden = true) UsuarioActual usuario)}.
 */
public record UsuarioActual(
        UUID usuarioId,
        String correo,
        Set<String> roles,
        UUID sucursalId,
        UUID choferId) {

    public UsuarioActual {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean tieneRol(String rol) {
        return roles.contains(rol);
    }

    public boolean tieneAlgunRol(String... rolesBuscados) {
        for (String rol : rolesBuscados) {
            if (roles.contains(rol)) {
                return true;
            }
        }
        return false;
    }

    public boolean esAdminSistema() {
        return tieneRol(Roles.ADMIN_SISTEMA);
    }

    /** ADMIN_SISTEMA ve todo; los demás solo los datos de su propia sucursal. */
    public void verificarAccesoSucursal(UUID sucursal) {
        if (esAdminSistema()) {
            return;
        }
        if (sucursalId == null || !sucursalId.equals(sucursal)) {
            throw new ProhibidoException("SUCURSAL_AJENA", "No tiene acceso a los datos de esta sucursal");
        }
    }
}
