package gt.cunoc.proyecto2.identity.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Asignación o traslado de personal publicado por Fleet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonalSucursalAsignado(

        UUID personalId,
        UUID usuarioId,
        UUID sucursalId,
        String rolCodigo,
        LocalDate fechaAsignacion

) {
}
