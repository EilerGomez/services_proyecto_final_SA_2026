package api_gateway.revocaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RegistroRevocacionesTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T15:00:00Z");
    private final RegistroRevocaciones registro =
            new RegistroRevocaciones(Duration.ofMinutes(65), Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void unTokenEmitidoAntesDeLaRevocacionYaNoVale() {
        registro.registrar("ana", AHORA.minusSeconds(60));

        assertThat(registro.revocado("ana", AHORA.minusSeconds(61))).isTrue();
        assertThat(registro.revocado("ana", AHORA.minusSeconds(60))).isFalse();   // el emitido después sí
        assertThat(registro.revocado("ana", null)).isTrue();                      // sin iat: no se arriesga
        assertThat(registro.revocado("luis", AHORA.minusSeconds(3600))).isFalse();
        assertThat(registro.revocado(null, AHORA)).isFalse();
    }

    @Test
    void seQuedaLaRevocacionMasReciente() {
        registro.registrar("ana", AHORA.minusSeconds(10));
        registro.registrar("ana", AHORA.minusSeconds(100));

        assertThat(registro.revocado("ana", AHORA.minusSeconds(50))).isTrue();
        assertThat(registro.cantidad()).isEqualTo(1);
    }

    @Test
    void ignoraDatosIncompletosYDescartaLasViejas() {
        registro.registrar(null, AHORA);
        registro.registrar(" ", AHORA);
        registro.registrar("ana", null);
        registro.registrar("vieja", AHORA.minus(Duration.ofMinutes(70)));   // ningún JWT vigente es tan viejo

        assertThat(registro.cantidad()).isZero();
    }
}
