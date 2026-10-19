package gt.cunoc.proyecto2.identity.dtos.personal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * R5b: Fleet asigna una cuenta que ya existe como personal de una sucursal (sin crear cuenta).
 *
 * <p>Idempotente por {@code solicitudId} (el id de la fila de personal en Fleet), igual que R5.
 * {@code sucursalNombre} solo se usa en el correo de aviso.
 */
public record VincularCuentaRequest(

        @NotNull
        UUID solicitudId,

        @NotNull
        UUID usuarioId,

        @NotBlank
        @Pattern(regexp = "^(ADMIN_SUCURSAL|CAJERO)$")
        String rol,

        @NotNull
        UUID sucursalId,

        @Size(max = 100)
        String sucursalNombre
) {
}
