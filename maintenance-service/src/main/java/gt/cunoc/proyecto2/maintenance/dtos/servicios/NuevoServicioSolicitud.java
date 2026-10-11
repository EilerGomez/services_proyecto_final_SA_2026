package gt.cunoc.proyecto2.maintenance.dtos.servicios;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NuevoServicioSolicitud(
        @NotNull UUID busId,
        @NotNull LocalDate fechaServicio,
        @Size(max = 150) String taller,
        @Size(max = 500) String observaciones,
        @NotNull @Size(min = 1) List<@Valid LineaServicioSolicitud> lineas) {

    public NuevoServicioSolicitud {
        taller = taller == null || taller.isBlank() ? null : taller.strip();
        observaciones = observaciones == null || observaciones.isBlank() ? null : observaciones.strip();
    }
}
