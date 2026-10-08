package gt.cunoc.proyecto2.fleet.clientes.trips;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.fleet.dtos.trips.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** R3 contra un Trips simulado (MockRestServiceServer). */
class ClienteTripsTest {

    private static final String URL_R3 = "http://trips.local/trips/interno/viajes/activos";

    private MockRestServiceServer trips;
    private ClienteTrips cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://trips.local");
        trips = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteTrips(builder.build(), new ConfiguracionClientes().registroCircuitos());
    }

    @Test
    void viajesActivosDeUnBus() {
        UUID busId = UUID.randomUUID();
        UUID viajeId = UUID.randomUUID();
        trips.expect(requestTo(URL_R3 + "?busId=" + busId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"cantidad":1,"viajes":[{"viajeId":"%s","estado":"PROGRAMADO",
                         "fechaSalidaProgramada":"2026-11-02T08:00:00-06:00","otroCampo":"se ignora"}]}
                        """.formatted(viajeId), MediaType.APPLICATION_JSON));

        ViajesActivosRespuesta respuesta = cliente.viajesActivosDelBus(busId);

        assertThat(respuesta.hayViajes()).isTrue();
        assertThat(respuesta.viajes()).singleElement()
                .satisfies(v -> assertThat(v.viajeId()).isEqualTo(viajeId));
        trips.verify();
    }

    @Test
    void viajesActivosDeUnChofer() {
        UUID choferId = UUID.randomUUID();
        trips.expect(requestTo(URL_R3 + "?choferId=" + choferId))
                .andRespond(withSuccess("{\"cantidad\":0,\"viajes\":[]}", MediaType.APPLICATION_JSON));

        assertThat(cliente.viajesActivosDelChofer(choferId).hayViajes()).isFalse();
        trips.verify();
    }

    @Test
    void siTripsFallaNoSePuedeDesactivar() {
        UUID busId = UUID.randomUUID();
        trips.expect(requestTo(URL_R3 + "?busId=" + busId)).andRespond(withServerError());

        VerificarError.lanza(() -> cliente.viajesActivosDelBus(busId), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }
}
