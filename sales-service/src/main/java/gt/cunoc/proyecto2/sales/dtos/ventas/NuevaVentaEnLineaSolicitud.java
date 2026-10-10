package gt.cunoc.proyecto2.sales.dtos.ventas;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NuevaVentaEnLineaSolicitud(
        @NotNull UUID viajeId,
        @NotEmpty @Size(max = 60) List<@NotNull @Positive Integer> asientos,
        @NotNull LocalDate fechaPago) {

    /** Los asientos pedidos, en orden. 400 ASIENTOS_REPETIDOS / ASIENTO_FUERA_DE_RANGO. */
    public List<Integer> asientosValidados(int capacidad) {
        return Asientos.validar(asientos, capacidad);
    }
}