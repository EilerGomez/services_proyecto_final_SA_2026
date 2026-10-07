package gt.cunoc.proyecto2.identity.evento.payload;

import java.util.UUID;

/**
 * Payload de PerfilActualizado.
 */
public record PerfilActualizadoPayload(

        UUID usuarioId,
        String nit,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        String fotoUrl,
        UUID choferId

) {
}
