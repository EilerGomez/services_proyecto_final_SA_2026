package gt.cunoc.proyecto2.identity.services.oauth;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Verifica el login con Google en el backend: intercambia el code por tokens
 * y valida la firma del id_token (RS256, JWKS de Google con caché) más
 * emisor, audiencia y vigencia. Nunca confía en lo que mande el frontend.
 */
@Service
public class VerificadorGoogle {

    public static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
    public static final String URL_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Duration JWKS_CACHE = Duration.ofHours(1);
    private static final Duration TOLERANCIA_RELOJ = Duration.ofMinutes(2);

    /** Cuenta de Google verificada (solo sub + email + email_verified abren sesión). */
    public record CuentaGoogle(String sub, String correo, boolean verificado, String nombre, String foto) { }

    private final ClienteHttpGoogle http;
    private final GoogleOAuthProperties propiedades;
    private volatile JWKSet jwks;
    private volatile Instant jwksVence = Instant.MIN;

    public VerificadorGoogle(ClienteHttpGoogle http, GoogleOAuthProperties propiedades) {
        this.http = http;
        this.propiedades = propiedades;
    }

    /** Intercambia el authorization code por el id_token (el code es de un solo uso en Google). */
    public String intercambiarCodigo(String codigo) {
        Map<String, Object> respuesta;
        try {
            respuesta = http.postFormulario(URL_TOKEN, Map.of(
                    "code", codigo,
                    "client_id", propiedades.getClientId(),
                    "client_secret", propiedades.getClientSecret(),
                    "redirect_uri", propiedades.getRedirectUri(),
                    "grant_type", "authorization_code"));
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException("GOOGLE_SIN_RESPUESTA",
                    "Google no respondió al intercambiar el código");
        }
        Object idToken = respuesta.get("id_token");
        if (!(idToken instanceof String token) || token.isBlank()) {
            throw new NoAutenticadoException("GOOGLE_SIN_ID_TOKEN", "Google no devolvió identidad");
        }
        return token;
    }

    /** Valida firma, emisor, audiencia y vigencia del id_token y devuelve sus claims. */
    public CuentaGoogle verificar(String idToken) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(idToken);
        } catch (ParseException e) {
            throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "La identidad de Google no es válida");
        }
        JWSHeader cabecera = jwt.getHeader();
        if (!"RS256".equals(cabecera.getAlgorithm().getName())) {
            throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "La identidad de Google no es válida");
        }
        verificarFirma(jwt, cabecera.getKeyID());
        JWTClaimsSet claims = claims(jwt);
        if (!EMISORES.contains(claims.getIssuer())) {
            throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "La identidad de Google no es válida");
        }
        if (!claims.getAudience().contains(propiedades.getClientId())) {
            throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "La identidad de Google no es válida");
        }
        Instant ahora = Instant.now();
        Date expira = claims.getExpirationTime();
        Date emitido = claims.getIssueTime();
        if (expira == null || !expira.toInstant().isAfter(ahora.minus(TOLERANCIA_RELOJ))
                || (emitido != null && emitido.toInstant().isAfter(ahora.plus(TOLERANCIA_RELOJ)))) {
            throw new NoAutenticadoException("GOOGLE_TOKEN_VENCIDO", "La identidad de Google venció");
        }
        Object verificado = claims.getClaim("email_verified");
        return new CuentaGoogle(
                claims.getSubject(),
                (String) claims.getClaim("email"),
                verificado instanceof Boolean b ? b : "true".equals(String.valueOf(verificado)),
                (String) claims.getClaim("name"),
                (String) claims.getClaim("picture"));
    }

    private void verificarFirma(SignedJWT jwt, String kid) {
        try {
            JWKSet llaves = llaves();
            JWK jwk = llaves.getKeyByKeyId(kid);
            if (!(jwk instanceof RSAKey rsa) || !jwt.verify(new RSASSAVerifier(rsa.toRSAPublicKey()))) {
                throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "La identidad de Google no es válida");
            }
        } catch (NoAutenticadoException e) {
            throw e;
        } catch (Exception e) {
            throw new ServicioNoDisponibleException("GOOGLE_SIN_RESPUESTA", "No se pudo validar con Google");
        }
    }

    private JWTClaimsSet claims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new NoAutenticadoException("GOOGLE_TOKEN_INVALIDO", "La identidad de Google no es válida");
        }
    }

    private synchronized JWKSet llaves() {
        if (jwks == null || !Instant.now().isBefore(jwksVence)) {
            try {
                jwks = JWKSet.parse(http.getTexto(URL_JWKS));
            } catch (RuntimeException | ParseException e) {
                throw new ServicioNoDisponibleException("GOOGLE_SIN_RESPUESTA",
                        "No se pudieron obtener las llaves de Google");
            }
            jwksVence = Instant.now().plus(JWKS_CACHE);
        }
        return jwks;
    }
}
