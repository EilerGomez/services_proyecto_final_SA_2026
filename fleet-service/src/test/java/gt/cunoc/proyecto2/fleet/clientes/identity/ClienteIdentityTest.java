package gt.cunoc.proyecto2.fleet.clientes.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.EstadoCuentaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** R5 contra un Identity simulado (MockRestServiceServer): sin red ni otro servicio levantado. */
class ClienteIdentityTest {

    private static final String URL_R5 = "http://identity.local/identity/interno/cuentas-personal";

    private MockRestServiceServer identity;
    private CircuitBreakerRegistry circuitos;
    private ClienteIdentity cliente;
    private SolicitudCuentaPersonal solicitud;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://identity.local");
        identity = MockRestServiceServer.bindTo(builder).build();
        circuitos = new ConfiguracionClientes().registroCircuitos();
        cliente = new ClienteIdentity(builder.build(), circuitos);
        solicitud = SolicitudCuentaPersonal.paraPersonal(UUID.randomUUID(), UUID.randomUUID(), RolPersonal.CAJERO,
                DatosPrueba.cuenta(null, null));
    }

    @Test
    void creaLaCuentaDelPersonal() {
        UUID usuarioId = UUID.randomUUID();
        identity.expect(requestTo(URL_R5))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"rol\":\"CAJERO\"")))
                .andRespond(withSuccess("""
                        {"usuarioId":"%s","correo":"ana@fleet.gt","nombreCompleto":"Ana Lopez","creada":true}
                        """.formatted(usuarioId), MediaType.APPLICATION_JSON));

        CuentaPersonalRespuesta respuesta = cliente.crearCuentaPersonal(solicitud);

        assertThat(respuesta.usuarioId()).isEqualTo(usuarioId);
        assertThat(respuesta.nombreCompleto()).isEqualTo("Ana Lopez");
        assertThat(respuesta.creada()).isTrue();
        identity.verify();
    }

    @Test
    void unCorreoRepetidoLlegaAlUsuarioConElMismoCodigo() {
        identity.expect(requestTo(URL_R5))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":409,"codigo":"CORREO_EN_USO","mensaje":"El correo ya existe",
                                 "ruta":"/identity/interno/cuentas-personal","timestamp":"2026-10-20T15:30:12Z",
                                 "correlationId":null,"errores":[]}
                                """));

        assertThatThrownBy(() -> cliente.crearCuentaPersonal(solicitud))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(409);
                    assertThat(e.getCodigo()).isEqualTo("CORREO_EN_USO");
                    assertThat(e.getMessage()).isEqualTo("El correo ya existe");
                });
    }

    @Test
    void unErrorDeIdentityEsServicioNoDisponible() {
        identity.expect(requestTo(URL_R5)).andRespond(withServerError());

        VerificarError.lanza(() -> cliente.crearCuentaPersonal(solicitud), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
    }

    @Test
    void unTokenRechazadoEsServicioNoDisponible() {
        identity.expect(requestTo(URL_R5)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        VerificarError.lanza(() -> cliente.crearCuentaPersonal(solicitud), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
    }

    @Test
    void unaRespuestaVaciaEsServicioNoDisponible() {
        identity.expect(requestTo(URL_R5)).andRespond(withSuccess());

        VerificarError.lanza(() -> cliente.crearCuentaPersonal(solicitud), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
    }

    @Test
    void consultaElEstadoDeLaCuenta() {
        UUID usuarioId = UUID.randomUUID();
        identity.expect(requestTo("http://identity.local/identity/interno/usuarios/" + usuarioId + "/estado"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"usuarioId":"%s","estado":"BLOQUEADO","bloqueadoHasta":"2026-10-20T15:00:00Z"}
                        """.formatted(usuarioId), MediaType.APPLICATION_JSON));

        EstadoCuentaRespuesta estado = cliente.estadoCuenta(usuarioId);

        assertThat(estado.estado()).isEqualTo("BLOQUEADO");
        assertThat(estado.bloqueadoHasta()).isEqualTo(Instant.parse("2026-10-20T15:00:00Z"));
        assertThat(estado.bloqueada(Instant.parse("2026-10-20T14:00:00Z"))).isTrue();
        assertThat(estado.bloqueada(Instant.parse("2026-10-20T16:00:00Z"))).isFalse();
        assertThat(estado.inactiva()).isFalse();
        identity.verify();
    }

    @Test
    void unaCuentaQueIdentityNoConoceEsServicioNoDisponible() {
        // 404: la cuenta no existe en Identity (inconsistencia entre servicios), el usuario recibe 503
        UUID usuarioId = UUID.randomUUID();
        identity.expect(requestTo("http://identity.local/identity/interno/usuarios/" + usuarioId + "/estado"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        VerificarError.lanza(() -> cliente.estadoCuenta(usuarioId), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
    }

    @Test
    void conElCircuitoAbiertoNoLlamaAIdentity() {
        circuitos.circuitBreaker("identity").transitionToOpenState();

        VerificarError.lanza(() -> cliente.crearCuentaPersonal(solicitud), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
        identity.verify();                                   // sin peticiones esperadas ni hechas
    }
}
