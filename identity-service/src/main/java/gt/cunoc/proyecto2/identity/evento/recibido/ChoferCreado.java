package gt.cunoc.proyecto2.identity.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Alta de chofer publicada por Fleet (solo los campos que usa Identity).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferCreado(

        UUID choferId,
        UUID usuarioId,
        UUID sucursalId

) {
}
