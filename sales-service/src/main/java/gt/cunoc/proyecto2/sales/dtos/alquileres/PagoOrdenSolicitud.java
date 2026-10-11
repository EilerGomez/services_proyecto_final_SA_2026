package gt.cunoc.proyecto2.sales.dtos.alquileres;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PagoOrdenSolicitud(@NotNull LocalDate fechaPago) {
}