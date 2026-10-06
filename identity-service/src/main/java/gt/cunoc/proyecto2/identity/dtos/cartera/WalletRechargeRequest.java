package gt.cunoc.proyecto2.identity.dtos.cartera;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * Recarga manual de cartera (fecha elegida por el usuario).
 */
public record WalletRechargeRequest(


        @NotNull
        @DecimalMin(value = "0.01", message = "debe ser mayor que cero")
        BigDecimal monto,


        LocalDate fechaMovimiento

) {
}
