package gt.cunoc.proyecto2.identity.dtos.usuario;

import java.util.UUID;

/**
 * Resultado de buscar una cuenta por correo exacto para asignarle un rol (personal de sucursal).
 * Solo datos mínimos para reconocer a la persona. Si no puede ser personal, {@code motivo}
 * explica por qué (el mismo mensaje que daría la asignación).
 */
public record UsuarioBusquedaResponse(

        UUID usuarioId,
        String correo,
        String nombreCompleto,
        String fotoUrl,
        boolean puedeSerPersonal,
        String motivo

) {
}
