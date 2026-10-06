package api_gateway.filter;

import api_gateway.constants.HeaderNames;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Propaga el contexto del JWT (claims de comun) y elimina headers falsificados del cliente. */
@Component
public class UserContextFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = exchange.mutate().request(request -> request.headers(headers -> {
            headers.remove(HeaderNames.USER_ID);
            headers.remove(HeaderNames.USER_ROLES);
            headers.remove(HeaderNames.USER_EMAIL);
            headers.remove(HeaderNames.SUCURSAL_ID);
            headers.remove(HeaderNames.CHOFER_ID);
        })).build();
        return sanitized.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken token && token.isAuthenticated())
                .cast(JwtAuthenticationToken.class)
                .map(token -> sanitized.mutate().request(request -> request.headers(headers -> {
                    var claims = token.getToken().getClaims();
                    set(headers, HeaderNames.USER_ID, token.getToken().getSubject());
                    set(headers, HeaderNames.USER_EMAIL, claims.get("correo"));
                    set(headers, HeaderNames.SUCURSAL_ID, claims.get("sucursalId"));
                    set(headers, HeaderNames.CHOFER_ID, claims.get("choferId"));
                    Set<String> roles = new LinkedHashSet<>();
                    addRoles(roles, claims.get("roles"));
                    if (!roles.isEmpty()) {
                        headers.set(HeaderNames.USER_ROLES, String.join(",", roles));
                    }
                })).build())
                .defaultIfEmpty(sanitized)
                .flatMap(chain::filter);
    }

    private void set(org.springframework.http.HttpHeaders headers, String name, Object value) {
        if (value != null && StringUtils.hasText(value.toString())) {
            headers.set(name, value.toString());
        }
    }

    private void addRoles(Set<String> roles, Object claim) {
        if (claim instanceof Collection<?> values) {
            values.stream().filter(String.class::isInstance).map(String.class::cast)
                    .filter(StringUtils::hasText).forEach(roles::add);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
