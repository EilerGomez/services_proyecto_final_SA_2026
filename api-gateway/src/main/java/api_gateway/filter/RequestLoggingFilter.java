package api_gateway.filter;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Registra una línea por solicitud al momento de confirmar la respuesta HTTP.
 *
 * El registro diferido con beforeCommit permite capturar el estado final, incluso cuando lo
 * establece el manejador global de errores, y vincularlo con el ID de correlación.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    // Registra metodo, ruta, estado final, latencia e ID al confirmar la respuesta.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long started = System.nanoTime();
        exchange.getResponse().beforeCommit(() -> {
            var status = exchange.getResponse().getStatusCode();
            log.info("method={} path={} status={} latencyMs={} correlationId={}",
                    exchange.getRequest().getMethod(), exchange.getRequest().getPath().value(),
                    status == null ? HttpStatus.OK.value() : status.value(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                    CorrelationIdFilter.resolve(exchange));
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    // Ordena el log después del filtro que asigna el ID de correlación.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
