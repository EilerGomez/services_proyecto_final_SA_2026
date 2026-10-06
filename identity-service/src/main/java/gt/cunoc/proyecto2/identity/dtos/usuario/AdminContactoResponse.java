package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.util.UUID;


/**
 * Administrador de sucursal para Notification (R4a).
 */
public record AdminContactoResponse(
        UUID usuarioId,
        String nombre,
        String correo) {
}
