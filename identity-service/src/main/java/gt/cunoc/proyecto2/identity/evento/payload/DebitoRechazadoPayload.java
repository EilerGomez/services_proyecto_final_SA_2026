package gt.cunoc.proyecto2.identity.evento.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de DebitoCarteraRechazado.
 */
public record DebitoRechazadoPayload(

        String referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        String motivo

) {
}
