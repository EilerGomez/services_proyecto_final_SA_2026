package api_gateway.security;

import api_gateway.constants.ErrorCodes;
import api_gateway.exception.ErrorResponseWriter;
import api_gateway.revocaciones.TokenNoRevocadoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
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
        if (esSesionRevocada(exception)) {
            // El front cierra la sesión y pide iniciar de nuevo (el refresh token ya se revocó en identity)
            return writer.write(exchange, HttpStatus.UNAUTHORIZED, ErrorCodes.SESION_REVOCADA,
                    TokenNoRevocadoValidator.MENSAJE);
        }
        return writer.write(exchange, HttpStatus.UNAUTHORIZED);
    }

    /** El validador de revocación falla con su código; llega envuelto (InvalidBearerTokenException → JwtValidationException). */
    static boolean esSesionRevocada(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof JwtValidationException validacion && validacion.getErrors().stream()
                    .anyMatch(e -> ErrorCodes.SESION_REVOCADA.equals(e.getErrorCode()))) {
                return true;
            }
            if (actual instanceof OAuth2AuthenticationException oauth
                    && ErrorCodes.SESION_REVOCADA.equals(oauth.getError().getErrorCode())) {
                return true;
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return false;
    }
}
