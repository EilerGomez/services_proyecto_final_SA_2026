package gt.cunoc.proyecto2.identity.evento.payload;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de RecuperacionContrasenaSolicitada.
 */
public record RecuperacionContrasenaPayload(

        UUID usuarioId,
        String correo,
        String nombre,
        String urlRecuperacion,
        Instant expiraEn

) {
}
