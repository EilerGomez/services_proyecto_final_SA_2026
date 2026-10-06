package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.util.Set;
import java.util.UUID;


/**
 * Perfil propio y detalle de usuario (I8, I12).
 */
public record PerfilResponse(
        UUID usuarioId,
        String correo,
        String nombres,
        String apellidos,
        String nit,
        String dpi,
        String telefono,
        String direccion,
        String fotoUrl,
        Set<String> roles,
        UUID sucursalId,
        UUID choferId) {
}
