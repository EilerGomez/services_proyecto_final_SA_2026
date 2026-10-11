package gt.cunoc.proyecto2.sales.clientes.trips;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.sales.dtos.trips.VendibleRespuesta;
import gt.cunoc.proyecto2.sales.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** R2 contra un Trips simulado (MockRestServiceServer): sin red ni otro servicio levantado. */
class ClienteTripsTest {

    private static final String URL_TRIPS = "http://trips.local";

    private MockRestServiceServer trips;
    private CircuitBreakerRegistry circuitos;
    private ClienteTrips cliente;
    private final UUID viajeId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL_TRIPS);
        trips = MockRestServiceServer.bindTo(builder).build();
        circuitos = new ConfiguracionClientes().registroCircuitos();
        cliente = new ClienteTrips(builder.build(), circuitos);
    }

    private String urlVendible() {
        return URL_TRIPS + "/trips/interno/viajes/" + viajeId + "/vendible";
    }

    private static String errorJson(int status, String codigo) {
        return """
                {"status":%d,"codigo":"%s","mensaje":"Mensaje de Trips","ruta":"/trips/interno",
                 "timestamp":"2026-10-20T15:30:12Z","correlationId":null,"errores":[]}
                """.formatted(status, codigo);
    }

    @Test
    void unViajeVendibleTraeSusDatos() {
        UUID sucursalId = UUID.randomUUID();
        trips.expect(requestTo(urlVendible()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"viajeId":"%s","vendible":true,"motivo":null,"estado":"PROGRAMADO","tipo":"REGULAR",
                         "fechaSalidaProgramada":"2026-11-05T08:00:00-06:00","precioBoleto":75.00,
                         "capacidad":40,"sucursalOrigenId":"%s","otroCampo":"se ignora"}
                        """.formatted(viajeId, sucursalId), MediaType.APPLICATION_JSON));

        VendibleRespuesta respuesta = cliente.consultarVendible(viajeId);

        assertThat(respuesta.viajeId()).isEqualTo(viajeId);
        assertThat(respuesta.vendible()).isTrue();
        assertThat(respuesta.motivo()).isNull();
        assertThat(respuesta.estado()).isEqualTo("PROGRAMADO");
        assertThat(respuesta.tipo()).isEqualTo("REGULAR");
        assertThat(respuesta.fechaSalidaProgramada())
                .isAtSameInstantAs(OffsetDateTime.parse("2026-11-05T08:00:00-06:00"));
        assertThat(respuesta.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(respuesta.capacidad()).isEqualTo(40);
        assertThat(respuesta.sucursalOrigenId()).isEqualTo(sucursalId);
        trips.verify();
    }

    @Test
    void unViajeNoVendibleTraeElMotivo() {
        trips.expect(requestTo(urlVendible()))
                .andRespond(withSuccess("""
                        {"viajeId":"%s","vendible":false,"motivo":"VIAJE_YA_INICIO","estado":"EN_RUTA",
                         "tipo":"REGULAR","fechaSalidaProgramada":null,"precioBoleto":null,"capacidad":null,
                         "sucursalOrigenId":null}
                        """.formatted(viajeId), MediaType.APPLICATION_JSON));

        VendibleRespuesta respuesta = cliente.consultarVendible(viajeId);

        assertThat(respuesta.vendible()).isFalse();
        assertThat(respuesta.motivo()).isEqualTo("VIAJE_YA_INICIO");
        assertThat(respuesta.capacidad()).isNull();
        trips.verify();
    }

    @Test
    void un422DeTripsLlegaConElMismoCodigoYNoCuentaComoFalla() {
        trips.expect(requestTo(urlVendible()))
                .andRespond(withStatus(HttpStatus.valueOf(422)).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(422, "FECHA_INVALIDA")));

        VerificarError.lanza(() -> cliente.consultarVendible(viajeId), "FECHA_INVALIDA");
        assertThat(circuitos.circuitBreaker("trips").getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void un404EsServicioNoDisponible() {
        trips.expect(requestTo(urlVendible()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(404, "RUTA_INEXISTENTE")));

        VerificarError.lanza(() -> cliente.consultarVendible(viajeId), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }

    @Test
    void unErrorDeTripsEsServicioNoDisponibleYCuentaEnSuCircuito() {
        trips.expect(requestTo(urlVendible())).andRespond(withServerError());

        VerificarError.lanza(() -> cliente.consultarVendible(viajeId), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
        assertThat(circuitos.circuitBreaker("trips").getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    void sinRedSeReintentaTresVecesYEsServicioNoDisponible() {
        trips.expect(ExpectedCount.times(3), requestTo(urlVendible()))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        VerificarError.lanza(() -> cliente.consultarVendible(viajeId), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
        trips.verify();
    }

    @Test
    void conElCircuitoAbiertoNoLlamaATrips() {
        circuitos.circuitBreaker("trips").transitionToOpenState();

        VerificarError.lanza(() -> cliente.consultarVendible(viajeId), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
        trips.verify();                                      // sin peticiones esperadas ni hechas
    }

    @Test
    void unaRespuestaVaciaEsServicioNoDisponible() {
        trips.expect(requestTo(urlVendible())).andRespond(withSuccess());

        VerificarError.lanza(() -> cliente.consultarVendible(viajeId), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }
}
