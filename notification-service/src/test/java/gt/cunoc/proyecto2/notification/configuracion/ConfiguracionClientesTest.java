package gt.cunoc.proyecto2.notification.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class ConfiguracionClientesTest {

    @Test
    void creaElClienteDeIdentity() {
        // Arrange
        ServicioJwt jwt = new ServicioJwt(JsonMapper.builder().build(), "a".repeat(32),
                Duration.ofMinutes(60), Clock.systemUTC());
        ClientesInternos clientes = new ClientesInternos(jwt, "notification-service");

        // Act
        RestClient cliente = new ConfiguracionClientes().clienteRestIdentity(clientes, "http://localhost:8081");

        // Assert
        assertThat(cliente).isNotNull();
    }
}
