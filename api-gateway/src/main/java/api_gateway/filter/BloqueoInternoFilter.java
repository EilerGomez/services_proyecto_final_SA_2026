package api_gateway.filter;

import api_gateway.exception.ErrorResponseWriter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Las rutas internas de servicio a servicio responden 404 desde afuera. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BloqueoInternoFilter implements WebFilter {

    private final ErrorResponseWriter writer;

    public BloqueoInternoFilter(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String[] partes = exchange.getRequest().getPath().value().split("/");
        if (partes.length > 2 && "interno".equals(partes[2])) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return writer.write(exchange, HttpStatus.NOT_FOUND);
        }
        return chain.filter(exchange);
    }
}
