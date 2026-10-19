package gt.cunoc.proyecto2.notification.clientes;

import java.util.UUID;

/** Administrador de una sucursal (respuesta de GET /identity/interno/sucursales/{id}/administradores). */
public record AdminContacto(

        UUID usuarioId,
        String nombre,
        String correo

) {
}
