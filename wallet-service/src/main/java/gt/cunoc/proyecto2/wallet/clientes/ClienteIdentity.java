package gt.cunoc.proyecto2.wallet.clientes;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.wallet.errores.ErrorCartera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Llamadas de Wallet a Identity.
 * R4c · GET /identity/interno/usuarios/{id}/estado: antes de debitar se verifica que la cuenta esté activa.
 * Va directo al servicio (no por el gateway) con token de servicio; lo agrega {@link ClientesInternos}.
 */
@Component
public class ClienteIdentity {

    private static final Logger log = LoggerFactory.getLogger(ClienteIdentity.class);
    private static final String RUTA_ESTADO_CUENTA = "/identity/interno/usuarios/{id}/estado";

    private final RestClient cliente;

    @Autowired
    public ClienteIdentity(ClientesInternos clientes,
                           @Value("${app.servicios.identity-url:http://localhost:8081}") String url) {
        this(clientes.crear(url));
    }

    ClienteIdentity(RestClient cliente) {
        this.cliente = cliente;
    }

    /**
     * @return true si la cuenta existe y está ACTIVA; false si no existe o está inactiva o bloqueada
     * @throws ServicioNoDisponibleException si Identity no responde: el evento se reintenta y luego va a la DLQ
     */
    public boolean cuentaActiva(UUID usuarioId) {
        try {
            EstadoCuenta estado = cliente.get()
                    .uri(RUTA_ESTADO_CUENTA, usuarioId)
                    .retrieve()
                    .body(EstadoCuenta.class);
            return estado != null && estado.activa();
        } catch (HttpClientErrorException.NotFound noExiste) {
            return false;
        } catch (RestClientException e) {
            log.warn("Identity no respondió el estado de la cuenta {}: {}", usuarioId, e.getMessage());
            throw ErrorCartera.IDENTITY_NO_DISPONIBLE.excepcion();
        }
    }
}
