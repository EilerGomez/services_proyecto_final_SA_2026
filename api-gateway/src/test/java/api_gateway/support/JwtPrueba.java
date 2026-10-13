package api_gateway.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.json.JsonMapper;

/**
 * Genera JWT con el mismo formato que ServicioJwt de comun (lo que emite identity):
 * cabecera {"alg":"HS256","typ":"JWT"}, claims sub, correo, roles[], sucursalId, choferId,
 * iat y exp en segundos, todo en Base64 URL sin relleno y firmado con HmacSHA256.
 */
public final class JwtPrueba {

    public static final String SECRETO = "secreto-de-pruebas-del-gateway-de-mas-de-32-caracteres";
    public static final String CABECERA_HS256 = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private JwtPrueba() { }

    /** Claims como los pone ServicioJwt.generar para una persona. */
    public static Map<String, Object> claims(UUID usuarioId, String correo, Instant emitido,
            Instant expira, UUID sucursalId, String... roles) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", usuarioId.toString());
        claims.put("correo", correo);
        claims.put("roles", new TreeSet<>(List.of(roles)));
        if (sucursalId != null) {
            claims.put("sucursalId", sucursalId.toString());
        }
        claims.put("iat", emitido.getEpochSecond());
        claims.put("exp", expira.getEpochSecond());
        return claims;
    }

    /** Token vigente una hora, firmado con {@code secreto}. */
    public static String vigente(String secreto, UUID usuarioId, UUID sucursalId, String... roles) {
        Instant ahora = Instant.now();
        return firmar(secreto, CABECERA_HS256,
                claims(usuarioId, "usuario@correo.com", ahora, ahora.plusSeconds(3600), sucursalId, roles));
    }

    /** Token que venció hace una hora. */
    public static String vencido(String secreto, UUID usuarioId, String... roles) {
        Instant ahora = Instant.now();
        return firmar(secreto, CABECERA_HS256,
                claims(usuarioId, "usuario@correo.com", ahora.minusSeconds(7200), ahora.minusSeconds(3600),
                        null, roles));
    }

    public static String firmar(String secreto, String cabeceraJson, Map<String, Object> claims) {
        String contenido = B64.encodeToString(cabeceraJson.getBytes(StandardCharsets.UTF_8))
                + "." + B64.encodeToString(JSON.writeValueAsBytes(claims));
        return contenido + "." + B64.encodeToString(hmac(secreto, contenido));
    }

    private static byte[] hmac(String secreto, String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el JWT de prueba", e);
        }
    }
}
