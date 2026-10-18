package gt.cunoc.proyecto2.wallet.evento.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de CarteraDebitada y CarteraReembolsada (mismo contrato que cuando la cartera estaba en Identity).
 */
public record MovimientoCarteraPayload(

        UUID movimientoId,
        String referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        BigDecimal saldoResultante,
        LocalDate fechaMovimiento

) {
}
