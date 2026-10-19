package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Identity: una cuenta que ya existía recibió un rol administrativo (ADMIN_SISTEMA, ADMIN_SUCURSAL
 * o CAJERO). sucursalId y sucursalNombre vienen vacíos para ADMIN_SISTEMA.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RolAsignado(
        UUID usuarioId,
        String correo,
        String nombre,
        String rol,
        UUID sucursalId,
        String sucursalNombre) {
}
