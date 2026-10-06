package api_gateway.exception;

import java.time.Instant;
import java.util.List;

/** Formato único de error del gateway (igual que comun: codigo, mensaje, ruta). */
public record ErrorResponse(int status, String codigo, String mensaje, String ruta,
                            Instant timestamp, String correlationId, List<Object> errores) { }
