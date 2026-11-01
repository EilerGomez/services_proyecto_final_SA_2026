package gt.cunoc.proyecto2.notification.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class ClienteIdentityTest {

    private MockRestServiceServer servidor;
    private ClienteIdentity cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteIdentity(builder.build());
    }

    @Test
    void traeLosAdministradoresDeLaSucursal() {
        // Arrange
        UUID sucursal = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        servidor.expect(requestTo("/identity/interno/sucursales/" + sucursal + "/administradores"))
                .andRespond(withSuccess("""
                        [{"usuarioId":"%s","nombre":"Admin Sucursal","correo":"admin@correo.com"}]
                        """.formatted(admin), MediaType.APPLICATION_JSON));

        // Act
        List<AdminContacto> administradores = cliente.administradores(sucursal);

        // Assert
        assertThat(administradores).containsExactly(
                new AdminContacto(admin, "Admin Sucursal", "admin@correo.com"));
        servidor.verify();
    }

    @Test
    void sinCuerpoDevuelveListaVacia() {
        // Arrange
        UUID sucursal = UUID.randomUUID();
        servidor.expect(requestTo("/identity/interno/sucursales/" + sucursal + "/administradores"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        // Act + Assert
        assertThat(cliente.administradores(sucursal)).isEmpty();
    }

    @Test
    void traeElContactoDeUnUsuario() {
        // Arrange
        UUID usuario = UUID.randomUUID();
        servidor.expect(requestTo("/identity/interno/usuarios/" + usuario + "/contacto"))
                .andRespond(withSuccess("""
                        {"usuarioId":"%s","nombre":"Juan Pérez","correo":"juan@correo.com","telefono":"5555-5555"}
                        """.formatted(usuario), MediaType.APPLICATION_JSON));

        // Act
        Optional<ContactoUsuario> contacto = cliente.contacto(usuario);

        // Assert
        assertThat(contacto).contains(
                new ContactoUsuario(usuario, "Juan Pérez", "juan@correo.com", "5555-5555"));
    }

    @Test
    void tras5FallasElCircuitoSeAbreYYaNoLlamaAIdentity() {
        // Arrange: Identity responde 500 y el mínimo configurado para evaluar son 5 llamadas
        UUID usuario = UUID.randomUUID();
        servidor.expect(ExpectedCount.times(5), requestTo("/identity/interno/usuarios/" + usuario + "/contacto"))
                .andRespond(withServerError());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> cliente.contacto(usuario)).isInstanceOf(HttpServerErrorException.class);
        }

        // Act + Assert: la sexta la corta el circuito, ya no sale a la red
        assertThatThrownBy(() -> cliente.contacto(usuario)).isInstanceOf(CallNotPermittedException.class);
        servidor.verify();   // exactamente 5 llamadas llegaron a Identity
    }

    @Test
    void elCircuitoEsElMismoParaLosDosEndpoints() {
        // Arrange: 5 fallas pidiendo administradores
        UUID sucursal = UUID.randomUUID();
        servidor.expect(ExpectedCount.times(5),
                        requestTo("/identity/interno/sucursales/" + sucursal + "/administradores"))
                .andRespond(withServerError());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> cliente.administradores(sucursal))
                    .isInstanceOf(HttpServerErrorException.class);
        }

        // Act + Assert: el circuito abierto también corta la consulta de contacto
        assertThatThrownBy(() -> cliente.contacto(UUID.randomUUID()))
                .isInstanceOf(CallNotPermittedException.class);
        servidor.verify();
    }

    @Test
    void unUsuarioQueNoExisteNoAbreElCircuito() {
        // Arrange: 404 está en ignoreExceptions, no es una caída del servicio.
        // Las dos expectativas van antes de la primera llamada: MockRestServiceServer no acepta
        // registrar más una vez que ya hubo peticiones reales.
        UUID usuario = UUID.randomUUID();
        UUID otro = UUID.randomUUID();
        servidor.expect(ExpectedCount.times(10), requestTo("/identity/interno/usuarios/" + usuario + "/contacto"))
                .andRespond(withResourceNotFound());
        servidor.expect(requestTo("/identity/interno/usuarios/" + otro + "/contacto"))
                .andRespond(withSuccess("""
                        {"usuarioId":"%s","nombre":"Ana","correo":"ana@correo.com","telefono":null}
                        """.formatted(otro), MediaType.APPLICATION_JSON));

        // Act
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> cliente.contacto(usuario))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        // Assert: el circuito siguió cerrado, así que la undécima llamada sí salió a Identity
        assertThat(cliente.contacto(otro)).isPresent();
        servidor.verify();
    }
}
