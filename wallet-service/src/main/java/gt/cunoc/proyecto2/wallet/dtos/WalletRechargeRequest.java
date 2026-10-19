package gt.cunoc.proyecto2.wallet.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Recarga de cartera (fecha elegida por el usuario). El cobro lo hace la pasarela configurada.
 */
public record WalletRechargeRequest(

        @NotNull
        @DecimalMin(value = "0.01", message = "debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "admite hasta 10 enteros y 2 decimales")
        BigDecimal monto,

        LocalDate fechaMovimiento

) {
}
