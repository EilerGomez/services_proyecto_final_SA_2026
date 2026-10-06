package api_gateway.filter;

import api_gateway.constants.HeaderNames;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** GlobalFilters run inside the handler, after authentication by Spring Security's WebFilters. */
@Component
public class UserContextFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = exchange.mutate().request(request -> request.headers(headers -> {
            headers.remove(HeaderNames.USER_ID);
            headers.remove(HeaderNames.USER_ROLES);
        })).build();
        return sanitized.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken token && token.isAuthenticated())
                .cast(JwtAuthenticationToken.class)
                .map(token -> sanitized.mutate().request(request -> request.headers(headers -> {
                    String subject = token.getToken().getSubject();
                    if (StringUtils.hasText(subject)) {
                        headers.set(HeaderNames.USER_ID, subject);
                    }
                    Set<String> roles = new LinkedHashSet<>();
                    addRoles(roles, token.getToken().getClaims().get("roles"));
                    Object realm = token.getToken().getClaims().get("realm_access");
                    if (realm instanceof Map<?, ?> claims) {
                        addRoles(roles, claims.get("roles"));
                    }
                    if (!roles.isEmpty()) {
                        headers.set(HeaderNames.USER_ROLES, String.join(",", roles));
                    }
                })).build())
                .defaultIfEmpty(sanitized)
                .flatMap(chain::filter);
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
