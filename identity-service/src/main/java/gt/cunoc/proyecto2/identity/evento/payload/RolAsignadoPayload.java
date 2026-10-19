package gt.cunoc.proyecto2.identity.evento.payload;

import java.util.UUID;

/**
 * Payload de RolAsignado: una cuenta que ya existía recibió un rol administrativo
 * (ADMIN_SISTEMA, ADMIN_SUCURSAL o CAJERO). Notification avisa por correo que vuelva a iniciar sesión.
 * sucursalId y sucursalNombre van vacíos para ADMIN_SISTEMA.
 */
public record RolAsignadoPayload(

        UUID usuarioId,
        String correo,
        String nombre,
        String rol,
        UUID sucursalId,
        String sucursalNombre

) {
}
