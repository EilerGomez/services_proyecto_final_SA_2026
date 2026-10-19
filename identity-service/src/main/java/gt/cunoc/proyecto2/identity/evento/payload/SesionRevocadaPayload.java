package gt.cunoc.proyecto2.identity.evento.payload;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload de SesionRevocada: los JWT del usuario emitidos antes de {@code tokensValidosDesde} ya no valen.
 * Lo consume el API Gateway para rechazarlos al instante (retiro de personal o piloto, cuenta desactivada
 * o bloqueada).
 */
public record SesionRevocadaPayload(

        UUID usuarioId,
        Instant tokensValidosDesde

) {
}
