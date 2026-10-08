package gt.cunoc.proyecto2.identity.evento.payload;


import java.time.Instant;
import java.util.UUID;


/**
 * Payload de CodigoDobleFactorSolicitado (el código viaja al correo, de un solo uso y 10 min).
 */
public record CodigoDobleFactorPayload(
        UUID usuarioId,
        String correo,
        String nombre,
        String codigo,
        Instant expiraEn) {
}
