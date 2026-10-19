package api_gateway.revocaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

class OyenteSesionRevocadaTest {

    private final RegistroRevocaciones registro = new RegistroRevocaciones(Duration.ofMinutes(65));
    private final OyenteSesionRevocada oyente = new OyenteSesionRevocada(registro, JsonMapper.builder().build());

    private static Message mensaje(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void registraLaRevocacionDelSobreConFechaIso() {
        Instant desde = Instant.now().minusSeconds(5);

        oyente.recibir(mensaje("""
                {"eventId":"e1","eventType":"SesionRevocada","payload":{"usuarioId":"ana","tokensValidosDesde":"%s"}}
                """.formatted(desde)));

        assertThat(registro.revocado("ana", desde.minusSeconds(1))).isTrue();
    }

    @Test
    void tambienAceptaLaFechaEnSegundos() {
        long segundos = Instant.now().getEpochSecond();

        oyente.recibir(mensaje("""
                {"eventType":"SesionRevocada","payload":{"usuarioId":"luis","tokensValidosDesde":%d.500}}
                """.formatted(segundos)));

        assertThat(registro.revocado("luis", Instant.ofEpochSecond(segundos))).isTrue();
        assertThat(registro.revocado("luis", Instant.ofEpochSecond(segundos + 1))).isFalse();
    }

    @Test
    void unMensajeIncompletoOIlegibleSeIgnoraSinFallar() {
        oyente.recibir(mensaje("{\"payload\":{\"usuarioId\":\"ana\"}}"));
        oyente.recibir(mensaje("{\"payload\":{\"usuarioId\":\"ana\",\"tokensValidosDesde\":\"ayer\"}}"));
        oyente.recibir(mensaje("{\"sinPayload\":true}"));
        oyente.recibir(mensaje("no es json"));

        assertThat(registro.cantidad()).isZero();
    }
}
