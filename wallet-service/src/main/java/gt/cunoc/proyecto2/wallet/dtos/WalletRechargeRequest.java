package gt.cunoc.proyecto2.wallet.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Recarga de cartera (fecha elegida por el usuario). El cobro lo hace la pasarela configurada.
 */
public record WalletRechargeRequest(

        @NotNull
        @DecimalMin(value = "0.01", message = "debe ser mayor que cero")
        BigDecimal monto,

        LocalDate fechaMovimiento

) {
}
