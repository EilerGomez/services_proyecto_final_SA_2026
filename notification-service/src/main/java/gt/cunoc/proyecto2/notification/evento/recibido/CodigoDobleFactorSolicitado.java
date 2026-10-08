package gt.cunoc.proyecto2.notification.evento.recibido;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;


/**
 * Código de doble factor publicado por Identity (un solo uso, 10 minutos).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodigoDobleFactorSolicitado(
        UUID usuarioId,
        String correo,
        String nombre,
        String codigo,
        Instant expiraEn) {
}
