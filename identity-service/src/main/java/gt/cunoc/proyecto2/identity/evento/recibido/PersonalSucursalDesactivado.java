package gt.cunoc.proyecto2.identity.evento.recibido;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;


/**
 * Desactivación de personal publicada por Fleet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonalSucursalDesactivado(
        UUID personalId,
        UUID usuarioId,
        UUID sucursalId,
        String rolCodigo,
        LocalDate fechaDesasignacion) {
}
