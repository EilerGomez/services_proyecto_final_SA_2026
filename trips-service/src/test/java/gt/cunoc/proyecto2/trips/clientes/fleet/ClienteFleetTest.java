package gt.cunoc.proyecto2.trips.clientes.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.trips.dtos.fleet.ChoferFleetRespuesta;
import gt.cunoc.proyecto2.trips.dtos.fleet.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.trips.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** R1a y R1b contra un Fleet simulado (MockRestServiceServer): sin red ni otro servicio levantado. */
class ClienteFleetTest {

    private static final String URL_FLEET = "http://fleet.local";
    private static final LocalDate FECHA_VIAJE = LocalDate.of(2026, 10, 25);

    private MockRestServiceServer fleet;
    private CircuitBreakerRegistry circuitos;
    private ClienteFleet cliente;
    private final UUID busId = UUID.randomUUID();
    private final UUID choferId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL_FLEET);
        fleet = MockRestServiceServer.bindTo(builder).build();
        circuitos = new ConfiguracionClientes().registroCircuitos();
        cliente = new ClienteFleet(builder.build(), circuitos);
    }

    private String urlBus() {
        return URL_FLEET + "/fleet/interno/buses/" + busId + "/disponibilidad";
    }

    private String urlChofer() {
        return URL_FLEET + "/fleet/interno/choferes/" + choferId + "?fecha=2026-10-25";
    }

    private static String errorJson(int status, String codigo) {
        return """
                {"status":%d,"codigo":"%s","mensaje":"Mensaje de Fleet","ruta":"/fleet/interno",
                 "timestamp":"2026-10-20T15:30:12Z","correlationId":null,"errores":[]}
                """.formatted(status, codigo);
    }

    @Test
    void disponibilidadDelBus() {
        UUID sucursalId = UUID.randomUUID();
        fleet.expect(requestTo(urlBus()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"busId":"%s","sucursalId":"%s","placa":"P-123ABC","capacidadPasajeros":45,
                         "estado":"ACTIVO","disponible":true,"motivo":null,"otroCampo":"se ignora"}
                        """.formatted(busId, sucursalId), MediaType.APPLICATION_JSON));

        Optional<DisponibilidadBusRespuesta> respuesta = cliente.disponibilidadDelBus(busId);

        assertThat(respuesta).hasValueSatisfying(bus -> {
            assertThat(bus.busId()).isEqualTo(busId);
            assertThat(bus.sucursalId()).isEqualTo(sucursalId);
            assertThat(bus.capacidadPasajeros()).isEqualTo(45);
            assertThat(bus.disponible()).isTrue();
            assertThat(bus.motivo()).isNull();
        });
        fleet.verify();
    }

    @Test
    void unBusQueNoExisteEsOptionalVacio() {
        fleet.expect(requestTo(urlBus()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(404, "BUS_NO_EXISTE")));

        assertThat(cliente.disponibilidadDelBus(busId)).isEmpty();
        fleet.verify();
    }

    @Test
    void otro404EsServicioNoDisponible() {
        fleet.expect(requestTo(urlBus()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(404, "RUTA_INEXISTENTE")));

        VerificarError.lanza(() -> cliente.disponibilidadDelBus(busId), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void un404SinCuerpoEsServicioNoDisponible() {
        fleet.expect(requestTo(urlBus())).andRespond(withStatus(HttpStatus.NOT_FOUND));

        VerificarError.lanza(() -> cliente.disponibilidadDelBus(busId), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void unErrorDeFleetEsServicioNoDisponible() {
        fleet.expect(requestTo(urlBus())).andRespond(withServerError());

        VerificarError.lanza(() -> cliente.disponibilidadDelBus(busId), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void sinRedSeReintentaTresVecesYEsServicioNoDisponible() {
        fleet.expect(ExpectedCount.times(3), requestTo(urlBus()))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        VerificarError.lanza(() -> cliente.disponibilidadDelBus(busId), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        fleet.verify();
    }

    @Test
    void conElCircuitoAbiertoNoLlamaAFleet() {
        circuitos.circuitBreaker("fleet").transitionToOpenState();

        VerificarError.lanza(() -> cliente.disponibilidadDelBus(busId), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        VerificarError.lanza(() -> cliente.choferParaViaje(choferId, FECHA_VIAJE),
                ErrorIntegracion.FLEET_NO_DISPONIBLE);
        fleet.verify();                                      // sin peticiones esperadas ni hechas
    }

    @Test
    void unaRespuestaVaciaEsServicioNoDisponible() {
        fleet.expect(requestTo(urlBus())).andRespond(withSuccess());

        VerificarError.lanza(() -> cliente.disponibilidadDelBus(busId), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    // ----- R1b -----

    @Test
    void choferParaViajeEnviaLaFecha() {
        UUID sucursalId = UUID.randomUUID();
        fleet.expect(requestTo(urlChofer()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"choferId":"%s","sucursalId":"%s","usuarioId":null,"nombreCompleto":"Juan Perez",
                         "estado":"ACTIVO","salarioBaseViaje":350.00,"fechaVencimientoLicencia":"2027-01-31",
                         "licenciaVigente":true,"tipoLicencia":"A"}
                        """.formatted(choferId, sucursalId), MediaType.APPLICATION_JSON));

        Optional<ChoferFleetRespuesta> respuesta = cliente.choferParaViaje(choferId, FECHA_VIAJE);

        assertThat(respuesta).hasValueSatisfying(chofer -> {
            assertThat(chofer.choferId()).isEqualTo(choferId);
            assertThat(chofer.nombreCompleto()).isEqualTo("Juan Perez");
            assertThat(chofer.salarioBaseViaje()).isEqualByComparingTo("350.00");
            assertThat(chofer.fechaVencimientoLicencia()).isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(chofer.licenciaVigente()).isTrue();
            assertThat(chofer.esActivo()).isTrue();
        });
        fleet.verify();
    }

    @Test
    void unChoferQueNoExisteEsOptionalVacio() {
        fleet.expect(requestTo(urlChofer()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(404, "CHOFER_NO_EXISTE")));

        assertThat(cliente.choferParaViaje(choferId, FECHA_VIAJE)).isEmpty();
        fleet.verify();
    }

    @Test
    void unChoferConCodigoDeBusNoExisteEsServicioNoDisponible() {
        // Cada llamada solo acepta su propio código de "no existe"
        fleet.expect(requestTo(urlChofer()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(404, "BUS_NO_EXISTE")));

        VerificarError.lanza(() -> cliente.choferParaViaje(choferId, FECHA_VIAJE),
                ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void un422DeFleetLlegaConElMismoCodigo() {
        fleet.expect(requestTo(urlChofer()))
                .andRespond(withStatus(HttpStatus.valueOf(422)).contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson(422, "FECHA_INVALIDA")));

        VerificarError.lanza(() -> cliente.choferParaViaje(choferId, FECHA_VIAJE), "FECHA_INVALIDA");
    }

    @Test
    void choferSinRespuestaDeFleetEsServicioNoDisponible() {
        fleet.expect(requestTo(urlChofer())).andRespond(withServerError());

        VerificarError.lanza(() -> cliente.choferParaViaje(choferId, FECHA_VIAJE),
                ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }
}
