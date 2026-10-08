package gt.cunoc.proyecto2.identity.services.comun;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos (refresh y recuperación): generación aleatoria y hash SHA-256.
 */
@Component
public class TokenOpacoService {

    private final SecureRandom aleatorio = new SecureRandom();

    public String generar() {
        byte[] datos = new byte[32];
        aleatorio.nextBytes(datos);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }

    public String calcularHash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }
}
