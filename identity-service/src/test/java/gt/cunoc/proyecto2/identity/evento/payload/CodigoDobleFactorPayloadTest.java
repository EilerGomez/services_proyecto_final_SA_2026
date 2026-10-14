package gt.cunoc.proyecto2.identity.evento.payload;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** CodigoDobleFactorPayload: record sin cubrir. */
class CodigoDobleFactorPayloadTest {

    @Test
    void conservaCampos() {
        UUID usuarioId = UUID.randomUUID();
        Instant expiraEn = Instant.parse("2026-09-21T12:00:00Z");

        var payload = new CodigoDobleFactorPayload(
                usuarioId, "ana@correo.com", "Ana López", "123456", expiraEn);

        assertThat(payload.usuarioId()).isEqualTo(usuarioId);
        assertThat(payload.correo()).isEqualTo("ana@correo.com");
        assertThat(payload.nombre()).isEqualTo("Ana López");
        assertThat(payload.codigo()).isEqualTo("123456");
        assertThat(payload.expiraEn()).isEqualTo(expiraEn);
    }
}
