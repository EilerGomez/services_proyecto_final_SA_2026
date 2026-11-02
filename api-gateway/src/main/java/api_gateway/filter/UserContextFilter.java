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

/**
 * Prepara los encabezados de identidad que los servicios reciben desde el gateway.
 *
 * Primero elimina cualquier valor enviado por el cliente para impedir suplantación, después
 * si Spring Security autenticó un JWT, copia sus claims verificados a los headers internos.
 *
 * En solicitudes públicas anónimas solo los elimina y reenvía el resto de la solicitud.
 */
@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    // Elimina encabezados de contexto no confiables y propaga los claims del principal JWT autenticado.
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

    // Añade al request solo claims con valor no vacío.
    private void set(org.springframework.http.HttpHeaders headers, String name, Object value) {
        if (value != null && StringUtils.hasText(value.toString())) {
            headers.set(name, value.toString());
        }
    }

    // Copia a un conjunto los roles de texto del claim recibido.
    private void addRoles(Set<String> roles, Object claim) {
        if (claim instanceof Collection<?> values) {
            values.stream().filter(String.class::isInstance).map(String.class::cast)
                    .filter(StringUtils::hasText).forEach(roles::add);
        }
    }

    // Ejecuta este filtro después de los filtros de correlación y logging.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
