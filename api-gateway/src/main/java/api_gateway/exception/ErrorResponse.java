package api_gateway.exception;

import java.time.Instant;
import java.util.List;

// Formato único del error
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        String ruta,
        Instant timestamp,
        String correlationId,
        List<Object> errores
) {}
