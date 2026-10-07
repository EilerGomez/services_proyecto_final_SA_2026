package gt.cunoc.proyecto2.notification.evento.recibido;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;


/**
 * Cuenta creada publicada por Identity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CuentaCreada(
        UUID usuarioId,
        String correo,
        String nombre,
        List<String> roles,
        String urlActivacion) {
}
