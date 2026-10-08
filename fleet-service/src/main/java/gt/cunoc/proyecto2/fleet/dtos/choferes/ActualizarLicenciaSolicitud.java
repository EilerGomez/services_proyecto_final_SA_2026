package gt.cunoc.proyecto2.fleet.dtos.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/** Cuerpo de F20 · PUT /fleet/choferes/{id}/licencia (renovación o corrección). */
public record ActualizarLicenciaSolicitud(
        @NotBlank @Pattern(regexp = "^[A-Z0-9-]{4,30}$", message = "de 4 a 30 letras, números o guion")
        String numeroLicencia,
        @NotNull TipoLicencia tipoLicencia,
        @NotNull LocalDate fechaVencimientoLicencia,
        @NotNull LocalDate fechaRegistro,
        @NotNull @PositiveOrZero Integer version) {

    public ActualizarLicenciaSolicitud {
        numeroLicencia = Normalizador.codigo(numeroLicencia);
    }

    public void aplicarA(ChoferEntidad chofer) {
        chofer.actualizarLicencia(numeroLicencia, tipoLicencia, fechaVencimientoLicencia);
    }
}