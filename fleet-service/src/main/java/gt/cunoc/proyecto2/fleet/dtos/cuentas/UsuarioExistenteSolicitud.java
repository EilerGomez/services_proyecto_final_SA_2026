package gt.cunoc.proyecto2.fleet.dtos.cuentas;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Persona que YA tiene cuenta en la plataforma (se registró como cliente): se le asigna el puesto
 * sin crear otra cuenta (R5b). usuarioId viene de la búsqueda por correo de Identity.
 *
 * solicitudId (opcional): igual que en DatosCuentaSolicitud, el frontend lo genera al abrir el
 * formulario y lo reenvía si reintenta.
 *
 * fotoUrl (opcional, solo choferes): foto nueva subida con F22; sin ella se usa la foto de perfil de la
 * cuenta (si no tiene, Identity responde FOTO_OBLIGATORIA).
 */
public record UsuarioExistenteSolicitud(
        UUID solicitudId,
        @NotNull UUID usuarioId,
        @Size(max = 500) String fotoUrl) {

    /** El JSON usa el constructor canónico (hay otro de conveniencia). */
    @JsonCreator
    public UsuarioExistenteSolicitud {
        fotoUrl = fotoUrl == null || fotoUrl.isBlank() ? null : fotoUrl.trim();
    }

    /** Personal de sucursal: sin foto. */
    public UsuarioExistenteSolicitud(UUID solicitudId, UUID usuarioId) {
        this(solicitudId, usuarioId, null);
    }
}
