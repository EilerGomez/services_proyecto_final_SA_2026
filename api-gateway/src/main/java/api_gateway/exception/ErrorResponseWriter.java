package api_gateway.exception;

import api_gateway.constants.ErrorCodes;
import api_gateway.constants.HeaderNames;
import api_gateway.filter.CorrelationIdFilter;
import java.time.Instant;
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
        return Mono.defer(() -> {
            var response = exchange.getResponse();
            if (response.isCommitted()) {
                return Mono.empty();
            }
            String correlationId = CorrelationIdFilter.resolve(exchange);
            HttpStatus knownStatus = HttpStatus.resolve(status.value());
            String message = knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase();
            ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), code(status.value()),
                    message, exchange.getRequest().getPath().value(), correlationId);
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
}
