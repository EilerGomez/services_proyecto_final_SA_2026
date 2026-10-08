package gt.cunoc.proyecto2.fleet.clientes.trips;

import gt.cunoc.proyecto2.fleet.clientes.LlamadasRemotas;
import gt.cunoc.proyecto2.fleet.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.fleet.dtos.trips.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Llamadas de Fleet a Trips.
 * R3 · GET /trips/interno/viajes/activos: viajes PROGRAMADO o EN_RUTA de un bus o de un chofer.
 * Se usa antes de desactivar un bus (F14) o un chofer (F21).
 */
@Component
public class ClienteTrips {

    private static final String RUTA_VIAJES_ACTIVOS = "/trips/interno/viajes/activos";

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    public ClienteTrips(@Qualifier(ConfiguracionClientes.CLIENTE_TRIPS) RestClient cliente,
                        CircuitBreakerRegistry circuitos) {
        this.cliente = cliente;
        this.circuito = circuitos.circuitBreaker("trips");
    }

    public ViajesActivosRespuesta viajesActivosDelBus(UUID busId) {
        return consultar("busId", busId);
    }

    public ViajesActivosRespuesta viajesActivosDelChofer(UUID choferId) {
        return consultar("choferId", choferId);
    }

    private ViajesActivosRespuesta consultar(String parametro, UUID id) {
        return LlamadasRemotas.ejecutar(circuito,
                () -> cliente.get()
                        .uri(uri -> uri.path(RUTA_VIAJES_ACTIVOS).queryParam(parametro, id).build())
                        .retrieve()
                        .body(ViajesActivosRespuesta.class),
                ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }
}