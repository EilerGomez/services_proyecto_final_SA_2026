package api_gateway.revocaciones;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.support.JwtPrueba;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import tools.jackson.databind.json.JsonMapper;

/** La cola propia del gateway, enlazada a SesionRevocada del exchange de identity. */
class ConfiguracionRevocacionesTest {

    private final ConfiguracionRevocaciones configuracion = new ConfiguracionRevocaciones();

    @Test
    void colaAnonimaEnlazadaASesionRevocada() {
        Queue cola = configuracion.colaRevocaciones();
        TopicExchange exchange = configuracion.eventosIdentity("identity.eventos");
        Binding enlace = configuracion.enlaceRevocaciones(cola, exchange);

        assertThat(cola.getName()).startsWith("api-gateway.revocaciones.");
        assertThat(cola.isAutoDelete()).isTrue();
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
        assertThat(enlace.getExchange()).isEqualTo("identity.eventos");
        assertThat(enlace.getRoutingKey()).isEqualTo("SesionRevocada");
        assertThat(enlace.getDestination()).isEqualTo(cola.getName());
    }

    @Test
    void tokenDeServicioYCargaInicialSeArmanConLaConfiguracion() {
        TokenServicio token = configuracion.tokenServicio(JwtPrueba.SECRETO, JsonMapper.builder().build());
        CargaInicialRevocaciones carga = new CargaInicialRevocaciones("http://identity:8081",
                token, new RegistroRevocaciones(Duration.ofMinutes(65)));

        assertThat(token.generar()).contains(".");
        assertThat(carga).isNotNull();
    }
}
