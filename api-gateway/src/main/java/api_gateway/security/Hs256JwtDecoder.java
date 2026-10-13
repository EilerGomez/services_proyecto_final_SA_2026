package api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Valida los JWT HS256 de comun (los emite Identity con ServicioJwt y el mismo JWT_SECRET).
 * <ul>
 *   <li>Llave: los bytes UTF-8 de {@code app.jwt.secret} con HmacSHA256, igual que ServicioJwt.</li>
 *   <li>Solo acepta {@code alg=HS256}; {@code typ} debe ser "JWT" o faltar (ServicioJwt pone "JWT").</li>
 *   <li>{@code exp} obligatorio y sin tolerancia de reloj, como ServicioJwt.validar
 *       (vencido si ahora &gt;= exp); {@code sub} obligatorio.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class Hs256JwtDecoder {

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secreto) {
        return crear(secreto);
    }

    static ReactiveJwtDecoder crear(String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(
                        new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator vigencia = new JwtTimestampValidator(Duration.ZERO);
        vigencia.setAllowEmptyExpiryClaim(false);
        JwtClaimValidator<Object> sujeto = new JwtClaimValidator<>(JwtClaimNames.SUB,
                valor -> valor != null && StringUtils.hasText(valor.toString()));
        // Agrega además los de siempre (typ JWT y cnf/x5t) que faltan en la lista.
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(List.of(vigencia, sujeto)));
        return decoder;
    }
}
