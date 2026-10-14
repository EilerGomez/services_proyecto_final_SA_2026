package gt.cunoc.proyecto2.identity.services.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Opcional: inicio de sesión con Google (Authorization Code en el backend).
 *
 * <p>Sin client-id configurado los endpoints OAuth responden 503 OAUTH_NO_CONFIGURADO
 * y el resto del servicio funciona igual. Las credenciales van por variables de entorno,
 * nunca en el repo (ver .env.example: GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.oauth2.google")
public class GoogleOAuthProperties {

    /** Client ID de la consola de Google (OAuth 2.0 Client IDs, tipo Web). */
    private String clientId = "";

    /** Client secret de la misma credencial. */
    private String clientSecret = "";

    /**
     * URL del callback del backend registrada en Google
     * (Authorized redirect URIs), p. ej. http://localhost:8080/identity/auth/oauth2/google/callback.
     */
    private String redirectUri = "";

    /** A dónde redirige el callback tras emitir el ticket (ruta /oauth/google/callback del frontend). */
    private String frontendUrl = "http://localhost:4200";

    /** Vigencia del state anti-CSRF (segundos). */
    private long stateTtlSegundos = 600;

    /** Vigencia del ticket de canje (segundos, un solo uso). */
    private long ticketTtlSegundos = 120;

    public boolean configurado() {
        return informado(clientId) && informado(clientSecret) && informado(redirectUri);
    }

    /**
     * Vacío, null o un placeholder sin resolver (${...}, que Boot deja literal
     * cuando la variable no existe y no hay default) cuentan como no configurado.
     */
    private static boolean informado(String valor) {
        return valor != null && !valor.isBlank() && !(valor.contains("${") && valor.contains("}"));
    }
}
