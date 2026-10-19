package gt.cunoc.proyecto2.identity.dtos.personal;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * R5b: Fleet asigna una cuenta que ya existe como personal de una sucursal o como chofer (sin crear cuenta).
 *
 * <p>Idempotente por {@code solicitudId} (el id de la fila de personal en Fleet, o el choferId), igual que R5.
 * {@code sucursalNombre} solo se usa en el correo de aviso. {@code fotoUrl} solo para CHOFER: reemplaza la
 * foto de perfil (si la cuenta no tiene foto, es obligatoria).
 */
public record VincularCuentaRequest(

        @NotNull
        UUID solicitudId,

        @NotNull
        UUID usuarioId,

        @NotBlank
        @Pattern(regexp = "^(ADMIN_SUCURSAL|CAJERO|CHOFER)$")
        String rol,

        @NotNull
        UUID sucursalId,

        @Size(max = 100)
        String sucursalNombre,

        @Size(max = 500)
        String fotoUrl
) {

    /** El JSON usa el constructor canónico (hay otro de conveniencia). */
    @JsonCreator
    public VincularCuentaRequest {
    }

    /** Personal de sucursal: sin foto. */
    public VincularCuentaRequest(UUID solicitudId, UUID usuarioId, String rol, UUID sucursalId,
                                 String sucursalNombre) {
        this(solicitudId, usuarioId, rol, sucursalId, sucursalNombre, null);
    }
}
