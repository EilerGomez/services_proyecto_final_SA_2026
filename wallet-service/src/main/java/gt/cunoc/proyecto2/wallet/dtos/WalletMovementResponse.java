package gt.cunoc.proyecto2.wallet.dtos;

import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;

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

    public static WalletMovementResponse de(MovimientoCartera movimiento) {
        return new WalletMovementResponse(movimiento.getId(), movimiento.getTipo(), movimiento.getMonto(),
                movimiento.getSaldoResultante(), movimiento.getTipoReferencia(), movimiento.getReferenciaId(),
                movimiento.getFechaMovimiento());
    }
}
