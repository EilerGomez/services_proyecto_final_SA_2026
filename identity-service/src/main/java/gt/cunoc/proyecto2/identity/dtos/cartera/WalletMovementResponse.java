package gt.cunoc.proyecto2.identity.dtos.cartera;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de un movimiento de cartera.
 */
public record WalletMovementResponse(

        UUID id,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoResultante,
        String tipoReferencia,
        UUID referenciaId,
        LocalDate fechaMovimiento

) {
}
