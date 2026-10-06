package api_gateway.security;

import api_gateway.exception.ErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
    private final ErrorResponseWriter writer;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return writer.write(exchange, HttpStatus.UNAUTHORIZED);
    }
}
