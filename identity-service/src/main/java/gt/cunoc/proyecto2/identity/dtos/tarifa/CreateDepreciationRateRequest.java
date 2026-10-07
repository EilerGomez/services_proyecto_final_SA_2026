package gt.cunoc.proyecto2.identity.dtos.tarifa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Creación de una tarifa de depreciación (solo INSERT).
 */
public record CreateDepreciationRateRequest(

        @NotNull
        @DecimalMin(value = "0.0001", message = "debe ser mayor que cero")
        BigDecimal montoPorKm,

        @NotNull
        LocalDate vigenteDesde

) {
}
