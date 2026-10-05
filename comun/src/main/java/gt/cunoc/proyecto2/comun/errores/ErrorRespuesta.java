package gt.cunoc.proyecto2.comun.errores;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de error de todos los microservicios.
 *
 * <pre>
 * { "status": 409, "codigo": "ASIENTO_OCUPADO", "mensaje": "El asiento 12 ya fue vendido",
 *   "ruta": "/sales/tickets", "timestamp": "...", "correlationId": "...", "errores": [] }
 * </pre>
 */
public record ErrorRespuesta(
        int status,
        String codigo,
        String mensaje,
        String ruta,
        Instant timestamp,
        String correlationId,
        List<ErrorCampo> errores) {

    public ErrorRespuesta {
        errores = errores == null ? List.of() : List.copyOf(errores);
    }

    /** Error de validación de un campo concreto. */
    public record ErrorCampo(String campo, String mensaje) {
    }
}
