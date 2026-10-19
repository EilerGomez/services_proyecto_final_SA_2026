package gt.cunoc.proyecto2.notification.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
}
