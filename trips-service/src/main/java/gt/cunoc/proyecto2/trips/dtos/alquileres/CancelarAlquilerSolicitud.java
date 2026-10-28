package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.utilidades.Normalizador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Cuerpo de POST /trips/alquileres/{id}/cancelar.
 *
 * <p>fechaHoraCancelacion es el momento que indica quien cancela y es el que decide qué tramo de la
 * tarifa de reembolso aplica. Se pide igual que la fecha de solicitud en T16 ("con la fecha actual
 * como valor inicial"), para poder probar la tabla de porcentajes sin esperar a que pase el tiempo.
 * Si no viene, se toma el momento actual.</p>
 */
public record CancelarAlquilerSolicitud(
        @NotBlank @Size(max = 255) String motivo,
        OffsetDateTime fechaHoraCancelacion,
        @NotNull @PositiveOrZero Integer version) {

    public CancelarAlquilerSolicitud {
        motivo = Normalizador.texto(motivo);
    }
}
