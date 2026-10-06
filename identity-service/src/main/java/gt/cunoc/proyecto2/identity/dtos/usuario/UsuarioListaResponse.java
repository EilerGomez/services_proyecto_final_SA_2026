package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.time.Instant;
import java.util.Set;
import java.util.UUID;


/**
 * Fila del listado de usuarios (I11) y base del detalle con estado (I12, I14).
 */
public record UsuarioListaResponse(
        UUID usuarioId,
        String correo,
        String nombre,
        String nit,
        Set<String> roles,
        String estado,
        UUID sucursalId,
        Instant ultimoAcceso) {
}
