package gt.cunoc.proyecto2.identity.dtos.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Cambio de estado de un usuario (ACTIVO, INACTIVO o BLOQUEADO).
 *
 * <p>Para BLOQUEADO se puede indicar {@code duracionMinutos}: la cuenta se desbloquea sola al vencer.
 * Sin duración (null), el bloqueo no vence y solo lo quita activar la cuenta de nuevo.
 */
public record UserStatusRequest(

        @NotBlank
        @Pattern(regexp = "^(ACTIVO|INACTIVO|BLOQUEADO)$", message = "debe ser ACTIVO, INACTIVO o BLOQUEADO")
        String estado,

        /** Minutos que dura el bloqueo (solo aplica a BLOQUEADO). Null = bloqueo sin vencimiento. */
        @Positive(message = "debe ser mayor que cero")
        Integer duracionMinutos

) {

    /** Bloqueo sin vencimiento, o un cambio a ACTIVO/INACTIVO. */
    public UserStatusRequest(String estado) {
        this(estado, null);
    }
}
