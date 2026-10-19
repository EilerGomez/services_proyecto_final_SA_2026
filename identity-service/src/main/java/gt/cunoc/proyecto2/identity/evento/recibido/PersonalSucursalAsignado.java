package gt.cunoc.proyecto2.identity.evento.recibido;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Asignación, traslado o reintegro de personal publicado por Fleet. sucursalNombre es opcional
 * (lo usa el correo de RolAsignado).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonalSucursalAsignado(

        UUID personalId,
        UUID usuarioId,
        UUID sucursalId,
        String rolCodigo,
        LocalDate fechaAsignacion,
        String sucursalNombre

) {

    /** El JSON usa el constructor canónico (hay otro de conveniencia). */
    @JsonCreator
    public PersonalSucursalAsignado {
    }

    public PersonalSucursalAsignado(UUID personalId, UUID usuarioId, UUID sucursalId, String rolCodigo,
                                    LocalDate fechaAsignacion) {
        this(personalId, usuarioId, sucursalId, rolCodigo, fechaAsignacion, null);
    }
}
