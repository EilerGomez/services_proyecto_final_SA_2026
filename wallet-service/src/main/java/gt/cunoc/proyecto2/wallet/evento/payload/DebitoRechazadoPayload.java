package gt.cunoc.proyecto2.wallet.evento.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de DebitoCarteraRechazado. motivo: SALDO_INSUFICIENTE, USUARIO_INACTIVO o CARTERA_INEXISTENTE.
 */
public record DebitoRechazadoPayload(

        String referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        String motivo

) {
}
