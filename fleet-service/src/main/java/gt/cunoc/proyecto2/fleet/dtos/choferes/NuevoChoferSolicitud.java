package gt.cunoc.proyecto2.fleet.dtos.choferes;

import com.fasterxml.jackson.annotation.JsonCreator;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.UsuarioExistenteSolicitud;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
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
 * Exactamente uno: cuenta (cuenta nueva, R5; cuenta.fotoUrl obligatoria, sale de F22 con tipo=CHOFER) o
 * usuarioExistente (persona ya registrada, R5b; la foto es opcional si su perfil ya tiene una).
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
        @Valid DatosCuentaSolicitud cuenta,
        @Valid UsuarioExistenteSolicitud usuarioExistente) {

    /** El JSON usa el constructor canónico (hay otro de conveniencia). */
    @JsonCreator
    public NuevoChoferSolicitud {
        numeroLicencia = Normalizador.codigo(numeroLicencia);
    }

    /** Chofer con cuenta nueva. */
    public NuevoChoferSolicitud(UUID sucursalId, String numeroLicencia, TipoLicencia tipoLicencia,
                                LocalDate fechaVencimientoLicencia, BigDecimal salarioBaseViaje,
                                LocalDate fechaRegistro, DatosCuentaSolicitud cuenta) {
        this(sucursalId, numeroLicencia, tipoLicencia, fechaVencimientoLicencia, salarioBaseViaje, fechaRegistro,
                cuenta, null);
    }

    /** true: usar una cuenta existente; false: crear una nueva. 400 CUENTA_REQUERIDA si no viene exactamente una. */
    public boolean conUsuarioExistente() {
        return ErrorPersonal.verificarUnaCuenta(cuenta, usuarioExistente);
    }

    /** El id del chofer es el solicitudId de R5/R5b: el mismo en cada reintento. */
    public UUID choferIdSolicitado() {
        UUID solicitudId = usuarioExistente != null ? usuarioExistente.solicitudId()
                : cuenta != null ? cuenta.solicitudId() : null;
        return solicitudId != null ? solicitudId : UUID.randomUUID();
    }

    public ChoferEntidad crearEntidad(UUID choferId, UUID usuarioId, String nombreCompleto) {
        return ChoferEntidad.registrar(choferId, sucursalId, usuarioId, nombreCompleto,
                numeroLicencia, tipoLicencia, fechaVencimientoLicencia, salarioBaseViaje);
    }
}
