package gt.cunoc.proyecto2.fleet.clientes.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.fleet.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.VinculoCuentaPersonal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** R5b contra un Identity simulado. */
class ClienteIdentityVinculoTest {

    private static final String URL_R5B = "http://identity.local/identity/interno/cuentas-personal/existente";

    private MockRestServiceServer identity;
    private ClienteIdentity cliente;
    private VinculoCuentaPersonal vinculo;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://identity.local");
        identity = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteIdentity(builder.build(), new ConfiguracionClientes().registroCircuitos());
        vinculo = new VinculoCuentaPersonal(UUID.randomUUID(), UUID.randomUUID(), "ADMIN_SUCURSAL",
                UUID.randomUUID(), "Xela Central");
    }

    @Test
    void vinculaLaCuentaExistente() {
        identity.expect(requestTo(URL_R5B))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"usuarioId\":\"" + vinculo.usuarioId() + "\"")))
                .andExpect(content().string(containsString("\"sucursalNombre\":\"Xela Central\"")))
                .andRespond(withSuccess("""
                        {"usuarioId":"%s","correo":"ana@fleet.gt","nombreCompleto":"Ana Lopez","creada":false}
                        """.formatted(vinculo.usuarioId()), MediaType.APPLICATION_JSON));

        CuentaPersonalRespuesta respuesta = cliente.vincularCuentaExistente(vinculo);

        assertThat(respuesta.usuarioId()).isEqualTo(vinculo.usuarioId());
        assertThat(respuesta.creada()).isFalse();
        identity.verify();
    }

    @Test
    void unaCuentaQueYaEsPersonalLlegaAlUsuarioConElMismoCodigo() {
        identity.expect(requestTo(URL_R5B))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":409,"codigo":"YA_ES_PERSONAL","mensaje":"La persona ya trabaja en una sucursal",
                                 "ruta":"/identity/interno/cuentas-personal/existente","timestamp":"2026-10-20T15:30:12Z",
                                 "correlationId":null,"errores":[]}
                                """));

        assertThatThrownBy(() -> cliente.vincularCuentaExistente(vinculo))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(409);
                    assertThat(e.getCodigo()).isEqualTo("YA_ES_PERSONAL");
                });
    }
}
