package api_gateway.exception;

import api_gateway.constants.ErrorCodes;
import api_gateway.constants.HeaderNames;
import api_gateway.filter.CorrelationIdFilter;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public Mono<Void> write(ServerWebExchange exchange, HttpStatusCode status) {
        return write(exchange, status, code(status.value()), mensaje(status.value()));
    }

    /** Con un código y mensaje propios (p. ej. 401 SESION_REVOCADA). */
    public Mono<Void> write(ServerWebExchange exchange, HttpStatusCode status, String codigo, String mensaje) {
        return Mono.defer(() -> {
            var response = exchange.getResponse();
            if (response.isCommitted()) {
                return Mono.empty();
            }
            String correlationId = CorrelationIdFilter.resolve(exchange);
            ErrorResponse body = new ErrorResponse(status.value(), codigo,
                    mensaje, exchange.getRequest().getPath().value(),
                    Instant.now(), correlationId, List.of());
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().setContentLength(bytes.length);
            response.getHeaders().set(HeaderNames.CORRELATION_ID, correlationId);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        });
    }

    private String code(int status) {
        return switch (status) {
            case 401 -> ErrorCodes.UNAUTHORIZED;
            case 403 -> ErrorCodes.FORBIDDEN;
            case 404 -> ErrorCodes.NOT_FOUND;
            case 503 -> ErrorCodes.SERVICE_UNAVAILABLE;
            case 504 -> ErrorCodes.GATEWAY_TIMEOUT;
            case 500 -> ErrorCodes.INTERNAL_ERROR;
            default -> "HTTP_" + status;
        };
    }

    private String mensaje(int status) {
        return switch (status) {
            case 401 -> "No autenticado";
            case 403 -> "Acceso denegado";
            case 404 -> "No encontrado";
            case 503 -> "Servicio no disponible";
            case 504 -> "Tiempo de espera agotado";
            case 500 -> "Error interno";
            default -> "La solicitud falló";
        };
    }
}
