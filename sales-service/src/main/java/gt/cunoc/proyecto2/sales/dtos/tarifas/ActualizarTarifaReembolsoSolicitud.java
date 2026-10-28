package gt.cunoc.proyecto2.sales.dtos.tarifas;

import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpo de PUT /sales/tarifas-reembolso/{id} (ADMIN_SISTEMA). El estado y el tramo de horas no se
 * editan: identifican la tarifa (UNIQUE). Para mover un tramo se desactiva la fila y se crea otra.
 */
public record ActualizarTarifaReembolsoSolicitud(
        @NotNull Boolean permiteCancelacion,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal porcentajeReembolso,
        @NotBlank @Size(max = 255) String justificacion,
        @NotNull Boolean activa,
        @NotNull @PositiveOrZero Integer version) {

    public ActualizarTarifaReembolsoSolicitud {
        justificacion = Normalizador.texto(justificacion);
    }
}
