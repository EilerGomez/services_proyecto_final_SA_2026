package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.models.Usuario;
import java.time.Instant;
import java.util.Optional;

/**
 * Reglas para dar un rol administrativo a una cuenta que ya existe (personal de sucursal o
 * administrador del sistema). Devuelven el impedimento (si hay) para que la búsqueda por correo
 * pueda mostrarlo sin lanzar y la asignación lo lance tal cual.
 */
public final class ReglasAsignacionRol {

    private ReglasAsignacionRol() {
    }

    /** ADMIN_SUCURSAL o CAJERO: cuenta utilizable, sin sucursal, sin rol de personal, no chofer ni admin del sistema. */
    public static Optional<NegocioException> impedimentoParaPersonal(Usuario usuario, Instant ahora) {
        Optional<NegocioException> estado = impedimentoPorEstado(usuario, ahora);
        if (estado.isPresent()) {
            return estado;
        }
        Optional<NegocioException> tipo = impedimentoPorTipoDeCuenta(usuario);
        if (tipo.isPresent()) {
            return tipo;
        }
        if (esPersonal(usuario)) {
            return Optional.of(new ConflictoException("YA_ES_PERSONAL",
                    "La persona ya trabaja en una sucursal"));
        }
        return Optional.empty();
    }

    /**
     * Cuentas que nunca pueden ser personal de sucursal: administradores del sistema y choferes
     * (también los retirados: conservan su choferId). Fleet lo consulta con R4c antes de reintegrar.
     */
    public static Optional<NegocioException> impedimentoPorTipoDeCuenta(Usuario usuario) {
        if (tieneRol(usuario, Roles.ADMIN_SISTEMA)) {
            return Optional.of(new ReglaNegocioException("ES_ADMIN_SISTEMA",
                    "La cuenta es de un administrador del sistema; no puede ser personal de sucursal"));
        }
        if (usuario.getChoferId() != null) {
            return Optional.of(new ReglaNegocioException("ES_CHOFER",
                    "La cuenta pertenece a un chofer; no puede ser personal de sucursal"));
        }
        return Optional.empty();
    }

    /** ADMIN_SISTEMA: cuenta utilizable, que no lo sea ya y que no sea personal de sucursal ni chofer. */
    public static Optional<NegocioException> impedimentoParaAdminSistema(Usuario usuario, Instant ahora) {
        Optional<NegocioException> estado = impedimentoPorEstado(usuario, ahora);
        if (estado.isPresent()) {
            return estado;
        }
        if (tieneRol(usuario, Roles.ADMIN_SISTEMA)) {
            return Optional.of(new ConflictoException("YA_ES_ADMIN_SISTEMA",
                    "La persona ya es administrador del sistema"));
        }
        if (usuario.getChoferId() != null) {
            return Optional.of(new ReglaNegocioException("ES_CHOFER",
                    "La cuenta pertenece a un chofer; no puede ser administrador del sistema"));
        }
        if (esPersonal(usuario)) {
            return Optional.of(new ReglaNegocioException("ES_PERSONAL_SUCURSAL",
                    "La persona trabaja en una sucursal; retírela primero desde Sucursales"));
        }
        return Optional.empty();
    }

    /** Un bloqueo vencido no impide (se quita solo en el siguiente inicio de sesión). */
    private static Optional<NegocioException> impedimentoPorEstado(Usuario usuario, Instant ahora) {
        String estado = usuario.getEstado();
        if ("BLOQUEADO".equals(estado)
                && (usuario.getBloqueadoHasta() == null || usuario.getBloqueadoHasta().isAfter(ahora))) {
            return Optional.of(new ReglaNegocioException("CUENTA_BLOQUEADA",
                    "La cuenta está bloqueada; espere a que venza el bloqueo o actívela en Usuarios"));
        }
        if (!"ACTIVO".equals(estado) && !"BLOQUEADO".equals(estado)) {
            return Optional.of(new ReglaNegocioException("CUENTA_INACTIVA",
                    "La cuenta está desactivada; actívela primero en Usuarios"));
        }
        return Optional.empty();
    }

    /** Tiene sucursal o rol de personal (ADMIN_SUCURSAL o CAJERO). */
    public static boolean esPersonal(Usuario usuario) {
        return usuario.getSucursalId() != null
                || tieneRol(usuario, Roles.ADMIN_SUCURSAL)
                || tieneRol(usuario, Roles.CAJERO);
    }

    private static boolean tieneRol(Usuario usuario, String codigo) {
        return usuario.getRoles().stream().anyMatch(rol -> codigo.equals(rol.getCodigo()));
    }
}
