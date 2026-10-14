package api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import api_gateway.support.JwtPrueba;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class Hs256JwtDecoderBranchesTest {

    @Test
    void secretoDe31CaracteresSeRechazaYElDe32SeAcepta() {
        assertThatThrownBy(() -> Hs256JwtDecoder.crear("a".repeat(31)))
                .isInstanceOf(IllegalStateException.class);
        assertThatNoException().isThrownBy(() -> Hs256JwtDecoder.crear("a".repeat(32)));
    }

    @Test
    void secretoVacioSeRechaza() {
        assertThatThrownBy(() -> Hs256JwtDecoder.crear(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretoMultibyteSeMideEnBytes() {
        // 16 "á" = 32 bytes UTF-8: válido; 15 = 30 bytes: inválido.
        assertThatNoException().isThrownBy(() -> Hs256JwtDecoder.crear("á".repeat(16)));
        assertThatThrownBy(() -> Hs256JwtDecoder.crear("á".repeat(15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretoDe32AceptaTokensFirmadosConEl() {
        String secreto = "b".repeat(32);
        ReactiveJwtDecoder decoder = Hs256JwtDecoder.crear(secreto);
        String token = JwtPrueba.vigente(secreto, UUID.randomUUID(), null, "CLIENTE");

        StepVerifier.create(decoder.decode(token))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isNotBlank())
                .verifyComplete();
    }

    @Test
    void subEnBlancoSeRechaza() {
        Instant ahora = Instant.now();
        Map<String, Object> claims = JwtPrueba.claims(UUID.randomUUID(), "x@correo.com", ahora,
                ahora.plusSeconds(600), null, "CLIENTE");
        claims.put("sub", "   ");
        String token = JwtPrueba.firmar(JwtPrueba.SECRETO, JwtPrueba.CABECERA_HS256, claims);
        ReactiveJwtDecoder decoder = Hs256JwtDecoder.crear(JwtPrueba.SECRETO);

        StepVerifier.create(Mono.defer(() -> decoder.decode(token)))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void subVacioSeRechaza() {
        Instant ahora = Instant.now();
        Map<String, Object> claims = JwtPrueba.claims(UUID.randomUUID(), "x@correo.com", ahora,
                ahora.plusSeconds(600), null, "CLIENTE");
        claims.put("sub", "");
        String token = JwtPrueba.firmar(JwtPrueba.SECRETO, JwtPrueba.CABECERA_HS256, claims);
        ReactiveJwtDecoder decoder = Hs256JwtDecoder.crear(JwtPrueba.SECRETO);

        StepVerifier.create(Mono.defer(() -> decoder.decode(token)))
                .expectError(JwtException.class)
                .verify();
    }
}
