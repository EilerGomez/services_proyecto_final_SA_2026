package api_gateway.exception;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

/**
 * Último manejador reactivo de errores no tratados por filtros o por el proxy HTTP.
 * Convierte fallos de conectividad, timeout y errores HTTP conocidos al contrato JSON del gateway,
 * evitando exponer detalles internos de excepciones al consumidor.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    private final ErrorResponseWriter writer;

    /**
     * Si la respuesta aún no se confirmó, determina el status y la escribe con el formato común.
     * Los errores 5xx se registran con ruta y tipo de excepción para facilitar diagnóstico.
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        HttpStatusCode status = statusOf(error);
        if (status.is5xxServerError()) {
            log.warn("Gateway failure status={} path={} type={}", status.value(),
                    exchange.getRequest().getPath().value(), error.getClass().getSimpleName());
        }
        return writer.write(exchange, status);
    }

    // Traduce causas conocidas a 4xx/5xx y usa 500 cuando no hay clasificación aplicable.
    private HttpStatusCode statusOf(Throwable error) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof ResponseStatusException statusException) {
                return statusException.getStatusCode();
            }
            if (cause instanceof java.util.concurrent.TimeoutException
                    || cause instanceof TimeoutException || cause instanceof SocketTimeoutException
                    || cause instanceof ConnectTimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
            if (cause instanceof ConnectException || cause instanceof UnknownHostException
                    || cause instanceof PrematureCloseException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
