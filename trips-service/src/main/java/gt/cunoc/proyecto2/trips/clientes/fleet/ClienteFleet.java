package gt.cunoc.proyecto2.trips.clientes.fleet;

import gt.cunoc.proyecto2.trips.clientes.LlamadasRemotas;
import gt.cunoc.proyecto2.trips.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.trips.dtos.fleet.ChoferFleetRespuesta;
import gt.cunoc.proyecto2.trips.dtos.fleet.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.trips.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Llamadas de Trips a Fleet (R1). Se usan al programar o cambiar un viaje (T10, T11, T22).
 * Son GET: es seguro reintentarlas. Optional vacío = el bus o chofer no existe en Fleet (404 *_NO_EXISTE).
 */
@Component
public class ClienteFleet {

    private static final String RUTA_DISPONIBILIDAD_BUS = "/fleet/interno/buses/{id}/disponibilidad";
    private static final String RUTA_CHOFER = "/fleet/interno/choferes/{id}";

    private static final Set<String> BUS_NO_EXISTE = Set.of("BUS_NO_EXISTE");
    private static final Set<String> CHOFER_NO_EXISTE = Set.of("CHOFER_NO_EXISTE");

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    public ClienteFleet(@Qualifier(ConfiguracionClientes.CLIENTE_FLEET) RestClient cliente,
                        CircuitBreakerRegistry circuitos) {
        this.cliente = cliente;
        this.circuito = circuitos.circuitBreaker("fleet");
    }

    public Optional<DisponibilidadBusRespuesta> disponibilidadDelBus(UUID busId) {
        return LlamadasRemotas.ejecutarOpcional(circuito,
                () -> cliente.get()
                        .uri(RUTA_DISPONIBILIDAD_BUS, busId)
                        .retrieve()
                        .body(DisponibilidadBusRespuesta.class),
                ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE);
    }

    public Optional<ChoferFleetRespuesta> choferParaViaje(UUID choferId, LocalDate fecha) {
        return LlamadasRemotas.ejecutarOpcional(circuito,
                () -> cliente.get()
                        .uri(uri -> uri.path(RUTA_CHOFER).queryParam("fecha", fecha).build(choferId))
                        .retrieve()
                        .body(ChoferFleetRespuesta.class),
                ErrorIntegracion.FLEET_NO_DISPONIBLE, CHOFER_NO_EXISTE);
    }
}