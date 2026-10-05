package api_gateway.filter;

import api_gateway.constants.HeaderNames;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** First in both chains: WebFilter covers security, actuator, CORS and unmatched paths. */
@Component
public class CorrelationIdFilter implements GlobalFilter, WebFilter, Ordered {
    private static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";

    public static String resolve(ServerWebExchange exchange) {
        return (String) exchange.getAttributes().computeIfAbsent(ATTRIBUTE, key -> {
            String supplied = exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID);
            return StringUtils.hasText(supplied) ? supplied : UUID.randomUUID().toString();
        });
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String id = resolve(exchange);
        exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, id);
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, id);
            return Mono.empty();
        });
        return chain.filter(withCorrelationId(exchange, id));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = resolve(exchange);
        exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, id);
        return chain.filter(withCorrelationId(exchange, id));
    }

    private ServerWebExchange withCorrelationId(ServerWebExchange exchange, String id) {
        return exchange.mutate().request(request ->
                request.headers(headers -> headers.set(HeaderNames.CORRELATION_ID, id))).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
