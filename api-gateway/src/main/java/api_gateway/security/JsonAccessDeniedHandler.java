package api_gateway.security;

import api_gateway.exception.ErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {
    private final ErrorResponseWriter writer;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
        return writer.write(exchange, HttpStatus.FORBIDDEN);
    }
}
