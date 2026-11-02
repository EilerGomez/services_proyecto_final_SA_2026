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

/**
 * Establece un ID único para seguir una solicitud de punta a punta.
 *
 * Implementa WebFilter para ejecutarse antes del despacho HTTP de WebFlux, incluso si la ruta
 * no se enruta, y GlobalFilter para asegurar que el ID también viaje con la llamada al servicio.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, WebFilter, Ordered {

    private static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";

    // Reutiliza el ID recibido o crea uno y lo almacena en la solicitud.
    public static String resolve(ServerWebExchange exchange) {
        return (String) exchange.getAttributes().computeIfAbsent(ATTRIBUTE, key -> {
            String supplied = exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID);
            return StringUtils.hasText(supplied) ? supplied : UUID.randomUUID().toString();
        });
    }

    // Añade correlación a toda solicitud HTTP antes del despacho de WebFlux.
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

    // Asegura que la solicitud reenviada por Gateway use el mismo identificador.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = resolve(exchange);
        exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, id);
        return chain.filter(withCorrelationId(exchange, id));
    }

    // Copia el encabezado de correlación a la solicitud reenviada.
    private ServerWebExchange withCorrelationId(ServerWebExchange exchange, String id) {
        return exchange.mutate().request(request ->
                request.headers(headers -> headers.set(HeaderNames.CORRELATION_ID, id))).build();
    }

    // Ejecuta el filtro al inicio de las cadenas reactivas.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
