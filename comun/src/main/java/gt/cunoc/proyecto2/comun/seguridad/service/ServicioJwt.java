package gt.cunoc.proyecto2.comun.seguridad.service;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Genera y valida JWT firmados con HS256 y el secreto compartido {@code app.jwt.secret} (≥ 32 caracteres).
 * Identity usa {@link #generar}; todos los servicios usan {@link #validar} (a través del filtro).
 *
 * <p>Claims: sub (usuarioId), correo, roles[], sucursalId, choferId, iat, exp.</p>
 */
@Component
public class ServicioJwt {

    private static final String ALGORITMO = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String CABECERA =
            B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final JsonMapper json;
    private final byte[] secreto;
    private final Duration duracion;
    private final Clock reloj;

    @Autowired
    public ServicioJwt(JsonMapper json,
                       @Value("${app.jwt.secret}") String secreto,
                       @Value("${app.jwt.duracion-minutos:60}") long duracionMinutos) {
        this(json, secreto, Duration.ofMinutes(duracionMinutos), Clock.systemUTC());
    }

    public ServicioJwt(JsonMapper json, String secreto, Duration duracion, Clock reloj) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
        this.json = json;
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.duracion = duracion;
        this.reloj = reloj;
    }

    public String generar(UsuarioActual usuario) {
        long ahora = reloj.instant().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", usuario.usuarioId().toString());
        claims.put("correo", usuario.correo());
        claims.put("roles", new TreeSet<>(usuario.roles()));
        if (usuario.sucursalId() != null) {
            claims.put("sucursalId", usuario.sucursalId().toString());
        }
        if (usuario.choferId() != null) {
            claims.put("choferId", usuario.choferId().toString());
        }
        claims.put("iat", ahora);
        claims.put("exp", ahora + duracion.toSeconds());

        String contenido = CABECERA + "." + B64.encodeToString(json.writeValueAsBytes(claims));
        return contenido + "." + B64.encodeToString(firmar(contenido));
    }

    /** @throws NoAutenticadoException si el token es inválido, está alterado o expiró. */
    public UsuarioActual validar(String token) {
        Map<String, Object> claims;
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                throw invalido();
            }
            Map<String, Object> cabecera = leer(partes[0]);
            if (!"HS256".equals(cabecera.get("alg"))) {           // evita alg=none y otros algoritmos
                throw invalido();
            }
            byte[] esperada = firmar(partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(esperada, B64D.decode(partes[2]))) {
                throw invalido();
            }
            claims = leer(partes[1]);
        } catch (IllegalArgumentException | JacksonException e) {
            throw invalido();
        }

        Object exp = claims.get("exp");
        if (!(exp instanceof Number numero) || reloj.instant().getEpochSecond() >= numero.longValue()) {
            throw new NoAutenticadoException("TOKEN_EXPIRADO", "La sesión expiró, vuelva a iniciar sesión");
        }

        try {
            return new UsuarioActual(
                    UUID.fromString((String) claims.get("sub")),
                    (String) claims.get("correo"),
                    roles(claims.get("roles")),
                    uuid(claims.get("sucursalId")),
                    uuid(claims.get("choferId")));
        } catch (ClassCastException | IllegalArgumentException | NullPointerException e) {
            throw invalido();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leer(String parte) {
        return json.readValue(B64D.decode(parte), Map.class);
    }

    private byte[] firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto, ALGORITMO));
            return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el JWT", e);
        }
    }

    private static Set<String> roles(Object valor) {
        if (valor instanceof Collection<?> lista) {
            return lista.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static NoAutenticadoException invalido() {
        return new NoAutenticadoException("TOKEN_INVALIDO", "El token no es válido");
    }
}
