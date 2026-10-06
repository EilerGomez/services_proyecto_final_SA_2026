package gt.cunoc.proyecto2.identity.evento.recibido;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;


/**
 * Desactivación de chofer publicada por Fleet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferDesactivado(


        UUID choferId,


        UUID usuarioId,


        UUID sucursalId

) {
}
