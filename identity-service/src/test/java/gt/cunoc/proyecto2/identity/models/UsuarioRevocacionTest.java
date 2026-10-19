package gt.cunoc.proyecto2.identity.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** La revocación cuenta desde el siguiente segundo completo: el iat del JWT va en segundos. */
class UsuarioRevocacionTest {

    @Test
    void conFraccionDeSegundoSeRedondeaAlSiguiente() {
        Usuario usuario = new Usuario();

        Instant desde = usuario.revocarTokens(Instant.parse("2026-10-20T15:00:00.300Z"));

        assertThat(desde).isEqualTo(Instant.parse("2026-10-20T15:00:01Z"));
        assertThat(usuario.getTokensValidosDesde()).isEqualTo(desde);
    }

    @Test
    void enUnSegundoExactoSeQuedaIgual() {
        Usuario usuario = new Usuario();

        assertThat(usuario.revocarTokens(Instant.parse("2026-10-20T15:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-20T15:00:00Z"));
    }
}
