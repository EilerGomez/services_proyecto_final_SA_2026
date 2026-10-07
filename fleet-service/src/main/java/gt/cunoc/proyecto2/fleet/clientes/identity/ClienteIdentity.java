package gt.cunoc.proyecto2.fleet.clientes.identity;

import gt.cunoc.proyecto2.fleet.clientes.Reintentos;
import gt.cunoc.proyecto2.fleet.clientes.TraductorErroresRemotos;
import gt.cunoc.proyecto2.fleet.configuracion.ConfiguracionClientes;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Llamadas de Fleet a Identity.
 * R5 · POST /identity/interno/cuentas-personal: crea la cuenta del personal (y del chofer en F18).
 *
 * <ul>
 *   <li>El token de servicio y el X-Correlation-Id los pone el RestClient (ClientesInternos de comun).</li>
 *   <li>Fallas de red: 3 intentos (Reintentos). Es seguro porque R5 es idempotente por solicitudId.</li>
 *   <li>Circuito "identity": si Identity falla seguido, se deja de llamar 30 s y se responde 503 de inmediato.</li>
 *   <li>400/409/422 de Identity (correo en uso, DPI repetido...) llegan al usuario con el mismo código.</li>
 * </ul>
 */
@Component
public class ClienteIdentity {

    private static final Logger log = LoggerFactory.getLogger(ClienteIdentity.class);
    private static final String RUTA_CUENTAS_PERSONAL = "/identity/interno/cuentas-personal";

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    public ClienteIdentity(@Qualifier(ConfiguracionClientes.CLIENTE_IDENTITY) RestClient cliente,
                           CircuitBreakerRegistry circuitos) {
        this.cliente = cliente;
        this.circuito = circuitos.circuitBreaker("identity");
    }

    public CuentaPersonalRespuesta crearCuentaPersonal(SolicitudCuentaPersonal solicitud) {
        try {
            return circuito.executeSupplier(() -> Reintentos.ejecutar(() -> cliente.post()
                    .uri(RUTA_CUENTAS_PERSONAL)
                    .body(solicitud)
                    .retrieve()
                    .body(CuentaPersonalRespuesta.class)));
        } catch (HttpClientErrorException e) {
            throw TraductorErroresRemotos.traducir(e, ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
        } catch (CallNotPermittedException | RestClientException e) {
            log.warn("R5 falló (Identity no disponible): {}", e.getMessage());
            throw ErrorIntegracion.IDENTITY_NO_DISPONIBLE.excepcion();
        }
    }
}