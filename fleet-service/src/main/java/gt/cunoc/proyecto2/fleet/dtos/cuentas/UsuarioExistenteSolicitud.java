package gt.cunoc.proyecto2.fleet.dtos.cuentas;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Persona que YA tiene cuenta en la plataforma (se registró como cliente): se le asigna el puesto
 * sin crear otra cuenta (R5b). usuarioId viene de la búsqueda por correo de Identity.
 *
 * solicitudId (opcional): igual que en DatosCuentaSolicitud, el frontend lo genera al abrir el
 * formulario y lo reenvía si reintenta.
 */
public record UsuarioExistenteSolicitud(
        UUID solicitudId,
        @NotNull UUID usuarioId) {
}
