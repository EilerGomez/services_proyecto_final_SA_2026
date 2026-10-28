package gt.cunoc.proyecto2.sales.dtos.tarifas;

import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Cuerpo de POST /sales/tarifas-reembolso (ADMIN_SISTEMA). */
public record NuevaTarifaReembolsoSolicitud(
        @NotNull EstadoAlquilerTarifa estadoAlquiler,
        @NotNull @PositiveOrZero Integer horasMinimasAntes,
        @NotNull Boolean permiteCancelacion,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal porcentajeReembolso,
        @NotBlank @Size(max = 255) String justificacion) {

    public NuevaTarifaReembolsoSolicitud {
        justificacion = Normalizador.texto(justificacion);
    }
}
