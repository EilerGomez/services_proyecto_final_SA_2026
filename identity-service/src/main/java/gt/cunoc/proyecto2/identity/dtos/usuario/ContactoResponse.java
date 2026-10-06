package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.util.UUID;


/**
 * Contacto de un usuario para Notification (R4b).
 */
public record ContactoResponse(
        UUID usuarioId,
        String nombre,
        String correo,
        String telefono) {
}
