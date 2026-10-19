package api_gateway.revocaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * El gateway arranca con las revocaciones ACTIVAS, como en producción (las demás pruebas las apagan).
 * Detecta beans que faltan en el arranque real. Sin RabbitMQ: el oyente no se inicia; la carga inicial
 * reintenta contra identity en segundo plano sin frenar el arranque.
 */
@SpringBootTest(properties = {
        "gateway.revocaciones.enabled=true",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "gateway.services.identity-url=http://127.0.0.1:1"
})
class ContextoConRevocacionesTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void arrancaConTodosLosBeansDeRevocacion() {
        assertThat(contexto.getBean(CargaInicialRevocaciones.class)).isNotNull();
        assertThat(contexto.getBean(OyenteSesionRevocada.class)).isNotNull();
        assertThat(contexto.getBean(TokenServicio.class)).isNotNull();
        assertThat(contexto.getBean(RegistroRevocaciones.class)).isNotNull();
    }
}
