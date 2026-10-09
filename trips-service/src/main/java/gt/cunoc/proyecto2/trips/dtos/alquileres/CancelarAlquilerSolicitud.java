package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.utilidades.Normalizador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /trips/alquileres/{id}/cancelar. */
public record CancelarAlquilerSolicitud(
        @NotBlank @Size(max = 255) String motivo,
        @NotNull @PositiveOrZero Integer version) {

    public CancelarAlquilerSolicitud {
        motivo = Normalizador.texto(motivo);
    }
}