package api_gateway.revocaciones;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.ObjectMapper;

/**
 * Token de servicio para llamar a los endpoints internos de identity (rol SERVICIO), con el mismo
 * formato que {@code ServicioJwt.generarServicio} de comun: HS256 con el mismo JWT_SECRET, sub estable
 * del servicio y 5 minutos de vigencia.
 */
public class TokenServicio {

    static final String NOMBRE = "api-gateway";
    static final Duration VIGENCIA = Duration.ofMinutes(5);

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final String CABECERA =
            B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final byte[] secreto;
    private final ObjectMapper json;
    private final Clock reloj;

    // Prepara la clave compartida, serializador y reloj para firmar tokens internos.
    public TokenServicio(String secreto, ObjectMapper json, Clock reloj) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.json = json;
        this.reloj = reloj;
    }

    // Igual que ServicioJwt.idServicio: siempre el mismo UUID para el mismo nombre.
    // Calcula el identificador estable del servicio gateway.
    static UUID idServicio() {
        return UUID.nameUUIDFromBytes(("servicio:" + NOMBRE).getBytes(StandardCharsets.UTF_8));
    }

    // Genera un JWT HS256 de rol SERVICIO con cinco minutos de vigencia.
    public String generar() {
        long ahora = reloj.instant().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", idServicio().toString());
        claims.put("correo", NOMBRE);
        claims.put("roles", List.of("SERVICIO"));
        claims.put("iat", ahora);
        claims.put("exp", ahora + VIGENCIA.toSeconds());
        String contenido = CABECERA + "." + B64.encodeToString(json.writeValueAsBytes(claims));
        return contenido + "." + B64.encodeToString(firmar(contenido));
    }

    // Firma el contenido del JWT usando HMAC-SHA256 y el secreto compartido.
    private byte[] firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el token de servicio", e);
        }
    }
}
