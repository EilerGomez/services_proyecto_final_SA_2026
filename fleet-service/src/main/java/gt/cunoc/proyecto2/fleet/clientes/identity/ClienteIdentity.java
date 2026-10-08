package gt.cunoc.proyecto2.fleet.clientes.identity;

import gt.cunoc.proyecto2.fleet.clientes.LlamadasRemotas;
import gt.cunoc.proyecto2.fleet.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Llamadas de Fleet a Identity.
 * R5 · POST /identity/interno/cuentas-personal: crea la cuenta del personal (y del chofer en F18).
 * Es seguro reintentar: Identity no crea dos cuentas con el mismo solicitudId.
 */
@Component
public class ClienteIdentity {

    private static final String RUTA_CUENTAS_PERSONAL = "/identity/interno/cuentas-personal";

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    public ClienteIdentity(@Qualifier(ConfiguracionClientes.CLIENTE_IDENTITY) RestClient cliente,
                           CircuitBreakerRegistry circuitos) {
        this.cliente = cliente;
        this.circuito = circuitos.circuitBreaker("identity");
    }

    public CuentaPersonalRespuesta crearCuentaPersonal(SolicitudCuentaPersonal solicitud) {
        return LlamadasRemotas.ejecutar(circuito,
                () -> cliente.post()
                        .uri(RUTA_CUENTAS_PERSONAL)
                        .body(solicitud)
                        .retrieve()
                        .body(CuentaPersonalRespuesta.class),
                ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
    }
}