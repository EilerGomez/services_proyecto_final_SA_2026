package api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
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

class Hs256JwtDecoderTest {

    private final ReactiveJwtDecoder decoder = Hs256JwtDecoder.crear(JwtPrueba.SECRETO);

    @Test
    void aceptaElTokenConElFormatoDeServicioJwt() {
        UUID usuario = UUID.randomUUID();
        UUID sucursal = UUID.randomUUID();
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, usuario, sucursal, "CAJERO");

        StepVerifier.create(decoder.decode(token))
                .assertNext(jwt -> {
                    assertThat(jwt.getSubject()).isEqualTo(usuario.toString());
                    assertThat(jwt.getClaimAsString("correo")).isEqualTo("usuario@correo.com");
                    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CAJERO");
                    assertThat(jwt.getClaimAsString("sucursalId")).isEqualTo(sucursal.toString());
                    assertThat(String.valueOf(jwt.getHeaders().get("alg"))).isEqualTo("HS256");
                })
                .verifyComplete();
    }

    @Test
    void aceptaElTokenDeServicioConRolServicio() {
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, UUID.randomUUID(), null, "SERVICIO");

        StepVerifier.create(decoder.decode(token))
                .assertNext(jwt -> assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SERVICIO"))
                .verifyComplete();
    }

    @Test
    void rechazaTokenVencido() {
        String token = JwtPrueba.vencido(JwtPrueba.SECRETO, UUID.randomUUID(), "CLIENTE");

        StepVerifier.create(Mono.defer(() -> decoder.decode(token)))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        String token = JwtPrueba.vigente("otro-secreto-distinto-con-mas-de-32-caracteres!!", UUID.randomUUID(),
                null, "CLIENTE");

        StepVerifier.create(Mono.defer(() -> decoder.decode(token)))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void rechazaAlgNone() {
        Instant ahora = Instant.now();
        Map<String, Object> claims = JwtPrueba.claims(UUID.randomUUID(), "x@correo.com", ahora,
                ahora.plusSeconds(600), null, "ADMIN_SISTEMA");
        String firmado = JwtPrueba.firmar(JwtPrueba.SECRETO, "{\"alg\":\"none\",\"typ\":\"JWT\"}", claims);
        String sinFirma = firmado.substring(0, firmado.lastIndexOf('.') + 1);

        StepVerifier.create(Mono.defer(() -> decoder.decode(sinFirma)))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void rechazaTokenSinExp() {
        Instant ahora = Instant.now();
        Map<String, Object> claims = JwtPrueba.claims(UUID.randomUUID(), "x@correo.com", ahora,
                ahora.plusSeconds(600), null, "CLIENTE");
        claims.remove("exp");
        String token = JwtPrueba.firmar(JwtPrueba.SECRETO, JwtPrueba.CABECERA_HS256, claims);

        StepVerifier.create(Mono.defer(() -> decoder.decode(token)))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void rechazaTokenSinSub() {
        Instant ahora = Instant.now();
        Map<String, Object> claims = JwtPrueba.claims(UUID.randomUUID(), "x@correo.com", ahora,
                ahora.plusSeconds(600), null, "CLIENTE");
        claims.remove("sub");
        String token = JwtPrueba.firmar(JwtPrueba.SECRETO, JwtPrueba.CABECERA_HS256, claims);

        StepVerifier.create(Mono.defer(() -> decoder.decode(token)))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void rechazaTextoQueNoEsJwt() {
        StepVerifier.create(Mono.defer(() -> decoder.decode("no-es-un-jwt")))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    void exigeSecretoDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> Hs256JwtDecoder.crear("corto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
        assertThatThrownBy(() -> Hs256JwtDecoder.crear(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
