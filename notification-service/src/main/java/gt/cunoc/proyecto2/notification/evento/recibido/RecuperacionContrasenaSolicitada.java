package gt.cunoc.proyecto2.notification.evento.recibido;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;


/**
 * Solicitud de recuperación publicada por Identity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecuperacionContrasenaSolicitada(
        UUID usuarioId,
        String correo,
        String nombre,
        String urlRecuperacion,
        Instant expiraEn) {
}
