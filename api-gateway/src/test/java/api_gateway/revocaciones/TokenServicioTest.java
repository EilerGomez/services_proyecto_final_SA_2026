package api_gateway.revocaciones;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.support.JwtPrueba;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

/** Mismo formato que ServicioJwt.generarServicio: identity lo acepta con rol SERVICIO. */
class TokenServicioTest {

    @Test
    void generaUnTokenDeServicioFirmadoConElSecreto() {
        String token = new TokenServicio(JwtPrueba.SECRETO, JsonMapper.builder().build(), Clock.systemUTC()).generar();
        var decoder = NimbusReactiveJwtDecoder.withSecretKey(new javax.crypto.spec.SecretKeySpec(
                JwtPrueba.SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();

        StepVerifier.create(decoder.decode(token))
                .assertNext(jwt -> {
                    assertThat(jwt.getSubject()).isEqualTo(
                            UUID.nameUUIDFromBytes("servicio:api-gateway".getBytes(StandardCharsets.UTF_8)).toString());
                    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SERVICIO");
                    assertThat(jwt.getClaimAsString("correo")).isEqualTo("api-gateway");
                    assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
                })
                .verifyComplete();
    }
}
