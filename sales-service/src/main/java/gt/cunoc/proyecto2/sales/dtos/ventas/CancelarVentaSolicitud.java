package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CancelarVentaSolicitud(
        @NotBlank @Size(max = 255) String motivo,
        @NotNull LocalDate fechaCancelacion,
        @NotNull Integer version) {

    public CancelarVentaSolicitud {
        motivo = Normalizador.texto(motivo);
    }
}