package gt.cunoc.proyecto2.fleet.dtos.identity;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import java.util.UUID;

/**
 * Cuerpo de R5b · POST /identity/interno/cuentas-personal/existente: asigna una cuenta existente
 * como ADMIN_SUCURSAL, CAJERO o CHOFER. solicitudId = id de la fila de personal_sucursal o choferId
 * (idempotencia). sucursalNombre solo se usa en el correo de aviso. fotoUrl solo para CHOFER.
 */
public record VinculoCuentaPersonal(
        UUID solicitudId,
        UUID usuarioId,
        String rol,
        UUID sucursalId,
        String sucursalNombre,
        String fotoUrl) {

    /** Personal de sucursal: sin foto. */
    public VinculoCuentaPersonal(UUID solicitudId, UUID usuarioId, String rol, UUID sucursalId,
                                 String sucursalNombre) {
        this(solicitudId, usuarioId, rol, sucursalId, sucursalNombre, null);
    }

    /** Chofer con cuenta existente: solicitudId = choferId. */
    public static VinculoCuentaPersonal paraChofer(UUID choferId, UUID usuarioId, UUID sucursalId,
                                                   String sucursalNombre, String fotoUrl) {
        return new VinculoCuentaPersonal(choferId, usuarioId, Roles.CHOFER, sucursalId, sucursalNombre, fotoUrl);
    }
}
