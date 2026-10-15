package gt.cunoc.proyecto2.identity.integracion.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

/**
 * Google falso (solo IT): /token intercambia codes conocidos por id_tokens firmados
 * con una llave propia y /certs publica su JWKS. Sin red externa.
 *
 * <ul>
 *   <li>{@code code=ok} -&gt; identidad verificada (sub google-it-1).</li>
 *   <li>{@code code=sin-verificar} -&gt; email_verified=false.</li>
 *   <li>otro code -&gt; 400 invalid_grant.</li>
 * </ul>
 */
final class GoogleFalso {

    static final String CORREO = "oauth-it@pruebas.gt";
    static final String SUB = "google-it-1";

    private final HttpServer servidor;
    private final RSAKey llave;
    private final String clienteId;

    private GoogleFalso(String clienteId) {
        this.clienteId = clienteId;
        try {
            llave = new RSAKeyGenerator(2048).keyID("falso-1").generate();
            servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo iniciar Google falso", e);
        }
        servidor.createContext("/token", this::token);
        servidor.createContext("/certs", this::certs);
        servidor.start();
    }

    static GoogleFalso iniciar(String clienteId) {
        return new GoogleFalso(clienteId);
    }

    String urlBase() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    private void token(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String codigo = Arrays.stream(cuerpo.split("&"))
                    .map(p -> p.split("=", 2))
                    .filter(p -> p.length == 2 && p[0].equals("code"))
                    .map(p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8))
                    .findFirst().orElse("");
            if (codigo.equals("ok") || codigo.equals("sin-verificar")) {
                responder(intercambio, 200, "{\"id_token\":\"" + idToken(codigo.equals("ok"))
                        + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
            } else {
                responder(intercambio, 400, "{\"error\":\"invalid_grant\"}");
            }
        }
    }

    private void certs(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            responder(intercambio, 200, "{\"keys\":[" + llave.toPublicJWK().toJSONString() + "]}");
        }
    }

    private String idToken(boolean verificado) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(SUB)
                    .issuer("accounts.google.com")
                    .audience(clienteId)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .issueTime(Date.from(Instant.now().minusSeconds(10)))
                    .claim("email", CORREO)
                    .claim("email_verified", verificado)
                    .claim("name", "OAuth Integración")
                    .claim("picture", "http://foto/it.jpg")
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("falso-1").build(), claims);
            jwt.sign(new RSASSASigner(llave));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el id_token falso", e);
        }
    }

    private static void responder(HttpExchange intercambio, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(status, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
