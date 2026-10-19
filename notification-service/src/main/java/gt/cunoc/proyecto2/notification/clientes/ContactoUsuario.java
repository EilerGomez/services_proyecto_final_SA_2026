package gt.cunoc.proyecto2.notification.clientes;

import java.util.UUID;

/** Contacto de un usuario (respuesta de GET /identity/interno/usuarios/{id}/contacto). */
public record ContactoUsuario(

        UUID usuarioId,
        String nombre,
        String correo,
        String telefono

) {
}
