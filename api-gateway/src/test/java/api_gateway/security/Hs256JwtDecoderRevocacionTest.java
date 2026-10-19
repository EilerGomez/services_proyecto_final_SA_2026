package api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.revocaciones.RegistroRevocaciones;
import api_gateway.revocaciones.TokenNoRevocadoValidator;
import api_gateway.support.JwtPrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

/** El decodificador con el validador de revocación, como lo arma el bean del gateway. */
class Hs256JwtDecoderRevocacionTest {

    private final RegistroRevocaciones registro = new RegistroRevocaciones(Duration.ofMinutes(65));
    private final ReactiveJwtDecoder decoder =
            Hs256JwtDecoder.crear(JwtPrueba.SECRETO, new TokenNoRevocadoValidator(registro));

    @Test
    void elTokenDeAntesDelRetiroSeRechazaConSesionRevocada() {
        UUID usuario = UUID.randomUUID();
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, usuario, null, "ADMIN_SUCURSAL");
        registro.registrar(usuario.toString(), Instant.now().plusSeconds(5));

        StepVerifier.create(decoder.decode(token))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(JwtValidationException.class);
                    assertThat(((JwtValidationException) error).getErrors())
                            .anyMatch(e -> "SESION_REVOCADA".equals(e.getErrorCode()));
                })
                .verify();
    }

    @Test
    void elTokenDeDespuesDelRetiroSiVale() {
        UUID usuario = UUID.randomUUID();
        registro.registrar(usuario.toString(), Instant.now().minusSeconds(60));
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, usuario, null, "CLIENTE");

        StepVerifier.create(decoder.decode(token))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo(usuario.toString()))
                .verifyComplete();
    }
}
