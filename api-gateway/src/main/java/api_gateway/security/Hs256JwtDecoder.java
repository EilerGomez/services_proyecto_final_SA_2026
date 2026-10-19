package api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import api_gateway.revocaciones.RegistroRevocaciones;
import api_gateway.revocaciones.TokenNoRevocadoValidator;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
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
 *   <li>No revocado: si a la persona la retiraron o desactivaron, sus JWT emitidos antes ya no valen
 *       ({@link TokenNoRevocadoValidator}).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class Hs256JwtDecoder {

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secreto, RegistroRevocaciones revocaciones) {
        return crear(secreto, new TokenNoRevocadoValidator(revocaciones));
    }

    /** Con validadores extra (además de firma, vigencia y sub). */
    @SafeVarargs
    static ReactiveJwtDecoder crear(String secreto, OAuth2TokenValidator<Jwt>... extra) {
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
        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>(List.of(vigencia, sujeto));
        validadores.addAll(List.of(extra));
        // Agrega además los de siempre (typ JWT y cnf/x5t) que faltan en la lista.
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(validadores));
        return decoder;
    }
}
