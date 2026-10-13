package api_gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Lee el Bearer token igual que Spring Security, salvo en las rutas públicas: ahí no lo
 * toma en cuenta y la petición sigue como anónima. Así un token vencido o inválido en una
 * ruta pública no provoca 401 en el gateway; el header Authorization no se toca y llega al
 * servicio, que lo valida (401 TOKEN_INVALIDO / TOKEN_EXPIRADO) o decide por rol (T8, T9).
 */
public class RutasPublicasBearerTokenConverter implements ServerAuthenticationConverter {

    private final ServerWebExchangeMatcher rutasPublicas;
    private final ServerAuthenticationConverter delegado;

    public RutasPublicasBearerTokenConverter(ServerWebExchangeMatcher rutasPublicas) {
        this(rutasPublicas, new ServerBearerTokenAuthenticationConverter());
    }

    public RutasPublicasBearerTokenConverter(ServerWebExchangeMatcher rutasPublicas,
            ServerAuthenticationConverter delegado) {
        Assert.notNull(rutasPublicas, "rutasPublicas no puede ser null");
        Assert.notNull(delegado, "delegado no puede ser null");
        this.rutasPublicas = rutasPublicas;
        this.delegado = delegado;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return rutasPublicas.matches(exchange)
                .flatMap(resultado -> resultado.isMatch()
                        ? Mono.<Authentication>empty()
                        : delegado.convert(exchange));
    }
}
