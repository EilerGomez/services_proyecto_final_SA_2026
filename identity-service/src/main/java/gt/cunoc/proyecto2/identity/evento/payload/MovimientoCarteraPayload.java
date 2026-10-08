package gt.cunoc.proyecto2.identity.evento.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de CarteraDebitada y CarteraReembolsada.
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
