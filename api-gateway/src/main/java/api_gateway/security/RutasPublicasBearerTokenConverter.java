package api_gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adapta la lectura del token Bearer para respetar la lista de rutas públicas del gateway.
 *
 * En ellas no autentica con el token, por lo que un token vencido no bloquea una consulta pública,
 * el encabezado Authorization se conserva y llega al servicio, que puede validarlo o autorizar
 * la operación según el rol. En rutas privadas delega en el convertidor estándar de Spring.
 */
public class RutasPublicasBearerTokenConverter implements ServerAuthenticationConverter {

    private final ServerWebExchangeMatcher rutasPublicas;
    private final ServerAuthenticationConverter delegado;

    // Usa el convertidor Bearer provisto por Spring Security.
    public RutasPublicasBearerTokenConverter(ServerWebExchangeMatcher rutasPublicas) {
        this(rutasPublicas, new ServerBearerTokenAuthenticationConverter());
    }

    // Conserva el matcher y el convertidor que procesará las rutas privadas.
    public RutasPublicasBearerTokenConverter(ServerWebExchangeMatcher rutasPublicas,
            ServerAuthenticationConverter delegado) {
        Assert.notNull(rutasPublicas, "rutasPublicas no puede ser null");
        Assert.notNull(delegado, "delegado no puede ser null");
        this.rutasPublicas = rutasPublicas;
        this.delegado = delegado;
    }

    // Devuelve autenticación vacía en rutas públicas y delega la lectura en el resto.
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return rutasPublicas.matches(exchange)
                .flatMap(resultado -> resultado.isMatch()
                        ? Mono.<Authentication>empty()
                        : delegado.convert(exchange));
    }
}
