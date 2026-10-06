package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.time.Instant;
import java.util.Set;
import java.util.UUID;


/**
 * Detalle de usuario con estado (I12, I14).
 */
public record UsuarioDetalleResponse(
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
        UUID choferId,
        String estado,
        Instant ultimoAcceso) {
}
