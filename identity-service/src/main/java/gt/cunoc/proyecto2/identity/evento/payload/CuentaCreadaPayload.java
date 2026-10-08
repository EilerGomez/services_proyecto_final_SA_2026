package gt.cunoc.proyecto2.identity.evento.payload;

import java.util.List;
import java.util.UUID;

/**
 * Payload de CuentaCreada.
 */
public record CuentaCreadaPayload(

        UUID usuarioId,
        String correo,
        String nombre,
        List<String> roles,
        String urlActivacion

) {
}
