package gt.cunoc.proyecto2.identity.evento.payload;

import java.util.List;
import java.util.UUID;

/**
 * Payload de CuentaCreada. sucursalNombre solo viene en cuentas de personal o choferes
 * (para que el correo de bienvenida diga dónde trabajará); los consumidores ignoran campos nuevos.
 */
public record CuentaCreadaPayload(

        UUID usuarioId,
        String correo,
        String nombre,
        List<String> roles,
        String urlActivacion,
        String sucursalNombre

) {

    public CuentaCreadaPayload(UUID usuarioId, String correo, String nombre, List<String> roles, String urlActivacion) {
        this(usuarioId, correo, nombre, roles, urlActivacion, null);
    }
}
