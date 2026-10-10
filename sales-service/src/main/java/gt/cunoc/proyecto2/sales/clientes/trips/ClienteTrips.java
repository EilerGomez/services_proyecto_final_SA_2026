package gt.cunoc.proyecto2.sales.clientes.trips;

import gt.cunoc.proyecto2.sales.clientes.LlamadasRemotas;
import gt.cunoc.proyecto2.sales.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.sales.dtos.trips.VendibleRespuesta;
import gt.cunoc.proyecto2.sales.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Llamada de Sales a Trips (R2): justo antes de vender, Trips confirma que el viaje sigue a la venta
 * (la copia local puede ir unos segundos atrasada). Es un GET: es seguro reintentarlo.
 */
@Component
public class ClienteTrips {

    private static final String RUTA_VENDIBLE = "/trips/interno/viajes/{id}/vendible";

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    public ClienteTrips(@Qualifier(ConfiguracionClientes.CLIENTE_TRIPS) RestClient cliente,
                        CircuitBreakerRegistry circuitos) {
        this.cliente = cliente;
        this.circuito = circuitos.circuitBreaker("trips");
    }

    /* R2: ¿se pueden vender boletos de este viaje? 503 TRIPS_NO_DISPONIBLE si Trips no responde. */
    public VendibleRespuesta consultarVendible(UUID viajeId) {
        return LlamadasRemotas.ejecutar(circuito,
                () -> cliente.get()
                        .uri(RUTA_VENDIBLE, viajeId)
                        .retrieve()
                        .body(VendibleRespuesta.class),
                ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }
}