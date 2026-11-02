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
 * Configuración de seguridad encargada de proporcionar el decodificador
 * reactivo de tokens JWT firmados mediante el algoritmo HS256.
 *
 * Configura la verificación criptográfica de los tokens utilizando
 * la clave secreta compartida con el servicio de identidad, garantizando
 * que únicamente se acepten tokens firmados con el algoritmo esperado.
 *
 * Además, establece las validaciones necesarias para comprobar la
 * integridad y vigencia de los JWT, exigir la presencia de los claims
 * obligatorios y verificar que los tokens no hayan sido revocados.
 *
 * De esta manera, centraliza las reglas de validación de autenticación
 * utilizadas por Spring Security para proteger los endpoints privados
 * del API Gateway.
 */
@Configuration(proxyBeanMethods = false)
public class Hs256JwtDecoder {

    /**
     * Registra el decodificador de JWT como componente de Spring, incorporando el validador encargado
     * de comprobar el estado de revocación de los tokens antes de autorizar el acceso.
     */
    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secreto, RegistroRevocaciones revocaciones) {
        return crear(secreto, new TokenNoRevocadoValidator(revocaciones));
    }

    /**
     * Crea el decoder HS256 y configura firma, algoritmo, expiración y sujeto obligatorios,
     * además de los validadores extra recibidos.
     */
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
