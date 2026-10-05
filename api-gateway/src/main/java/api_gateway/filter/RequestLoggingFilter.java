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

/** After correlation; beforeCommit observes the final status, including exception-handler errors. */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
