package gt.cunoc.proyecto2.identity.dtos.usuario;

import java.util.UUID;

/**
 * Cuenta que puede tomar un puesto (lista para elegir sin escribir el correo). Solo datos mínimos para
 * reconocer a la persona: la ven también los administradores de sucursal.
 */
public record UsuarioAsignableResponse(

        UUID usuarioId,
        String correo,
        String nombreCompleto,
        String fotoUrl

) {
}
