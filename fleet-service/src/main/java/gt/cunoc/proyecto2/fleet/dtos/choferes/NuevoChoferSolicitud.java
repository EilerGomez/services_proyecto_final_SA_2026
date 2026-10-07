package gt.cunoc.proyecto2.fleet.dtos.choferes;

import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuerpo de F18 · POST /fleet/choferes.
 * cuenta.fotoUrl es obligatoria (sale de F22 con tipo=CHOFER).
 * fechaRegistro: fecha de negocio de ChoferCreado y LicenciaChoferActualizada.
 */
public record NuevoChoferSolicitud(
        @NotNull UUID sucursalId,
        @NotBlank @Pattern(regexp = "^[A-Z0-9-]{4,30}$", message = "de 4 a 30 letras, números o guion")
        String numeroLicencia,
        @NotNull TipoLicencia tipoLicencia,
        @NotNull LocalDate fechaVencimientoLicencia,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal salarioBaseViaje,
        @NotNull LocalDate fechaRegistro,
        @NotNull @Valid DatosCuentaSolicitud cuenta) {

    public NuevoChoferSolicitud {
        numeroLicencia = Normalizador.codigo(numeroLicencia);
    }

    public ChoferEntidad crearEntidad(UUID choferId, UUID usuarioId, String nombreCompleto) {
        return ChoferEntidad.registrar(choferId, sucursalId, usuarioId, nombreCompleto,
                numeroLicencia, tipoLicencia, fechaVencimientoLicencia, salarioBaseViaje);
    }
}