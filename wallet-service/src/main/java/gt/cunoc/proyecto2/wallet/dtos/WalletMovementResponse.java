package gt.cunoc.proyecto2.wallet.dtos;

import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de un movimiento de cartera. {@code fechaMovimiento} es la fecha de negocio que eligió el usuario y
 * {@code fechaRegistro} el momento real en que se registró (da la hora y el orden dentro del mismo día).
 */
public record WalletMovementResponse(

        UUID id,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoResultante,
        String tipoReferencia,
        UUID referenciaId,
        LocalDate fechaMovimiento,
        Instant fechaRegistro

) {

    /** Sin hora de registro. */
    public WalletMovementResponse(UUID id, String tipo, BigDecimal monto, BigDecimal saldoResultante,
                                  String tipoReferencia, UUID referenciaId, LocalDate fechaMovimiento) {
        this(id, tipo, monto, saldoResultante, tipoReferencia, referenciaId, fechaMovimiento, null);
    }

    public static WalletMovementResponse de(MovimientoCartera movimiento) {
        return new WalletMovementResponse(movimiento.getId(), movimiento.getTipo(), movimiento.getMonto(),
                movimiento.getSaldoResultante(), movimiento.getTipoReferencia(), movimiento.getReferenciaId(),
                movimiento.getFechaMovimiento(), movimiento.getFechaCreacion());
    }
}
