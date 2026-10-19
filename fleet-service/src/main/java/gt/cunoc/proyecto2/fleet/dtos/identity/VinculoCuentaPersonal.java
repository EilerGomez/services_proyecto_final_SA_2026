package gt.cunoc.proyecto2.fleet.dtos.identity;

import java.util.UUID;

/**
 * Cuerpo de R5b · POST /identity/interno/cuentas-personal/existente: asigna una cuenta existente
 * como ADMIN_SUCURSAL o CAJERO. solicitudId = id de la fila de personal_sucursal (idempotencia).
 * sucursalNombre solo se usa en el correo de aviso.
 */
public record VinculoCuentaPersonal(
        UUID solicitudId,
        UUID usuarioId,
        String rol,
        UUID sucursalId,
        String sucursalNombre) {
}
