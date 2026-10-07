package gt.cunoc.proyecto2.fleet.dtos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Respuesta de R5. creada = false cuando Identity ya había procesado ese solicitudId
 * (reintento): devuelve la misma cuenta.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CuentaPersonalRespuesta(
        UUID usuarioId,
        String correo,
        String nombreCompleto,
        boolean creada) {
}