package gt.cunoc.proyecto2.notification.clientes;

import gt.cunoc.proyecto2.notification.configuracion.ConfiguracionClientes;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/*
 * Llamadas de Notification a Identity (R4):
 *   - administradores de una sucursal (aviso de ruta actualizada, para que revisen el precio del boleto),
 *   - contacto de un usuario (correo del piloto al asignarle un viaje o un alquiler privado,
 *     y recordatorio de vencimiento de licencia del chofer).
 * Las dos pasan por el circuito "identity": si Identity se cae, se corta la llamada en vez de
 * esperar 3 s por cada evento que haya que avisar.
 */
@Component
public class ClienteIdentity {

    private static final Logger log = LoggerFactory.getLogger(ClienteIdentity.class);
    private static final String RUTA_ADMINISTRADORES = "/identity/interno/sucursales/{id}/administradores";
    private static final String RUTA_CONTACTO = "/identity/interno/usuarios/{id}/contacto";

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    /** @Autowired es obligatorio: la clase tiene varios constructores y Spring no puede elegir solo. */
    @Autowired
    public ClienteIdentity(@Qualifier(ConfiguracionClientes.CLIENTE_IDENTITY) RestClient cliente,
                           CircuitBreakerRegistry circuitos) {
        this(cliente, circuitos.circuitBreaker("identity"));
    }

    /** Para las pruebas: circuito propio de la instancia, así un test no le deja el circuito abierto a otro. */
    ClienteIdentity(RestClient cliente) {
        this(cliente, CircuitBreakerRegistry.of(ConfiguracionClientes.configuracionDeCircuitos())
                .circuitBreaker("identity"));
    }

    ClienteIdentity(RestClient cliente, CircuitBreaker circuito) {
        this.cliente = cliente;
        this.circuito = circuito;
    }

    /** Administradores activos de la sucursal; lista vacía si la respuesta viene sin cuerpo. */
    public List<AdminContacto> administradores(UUID sucursalId) {
        List<AdminContacto> administradores = porElCircuito(() -> cliente.get()
                .uri(RUTA_ADMINISTRADORES, sucursalId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AdminContacto>>() { }));
        return administradores == null ? List.of() : administradores;
    }

    /** Contacto del usuario; vacío si Identity no lo tiene (la cuenta pudo eliminarse). */
    public Optional<ContactoUsuario> contacto(UUID usuarioId) {
        return Optional.ofNullable(porElCircuito(() -> cliente.get()
                .uri(RUTA_CONTACTO, usuarioId)
                .retrieve()
                .body(ContactoUsuario.class)));
    }

    /**
     * La falla se propaga igual que antes de tener circuito: el manejador del evento la deja reintentarse
     * y después va a la DLQ, y el recordatorio de licencia la atrapa y lo vuelve a intentar más tarde.
     * Lo único que agrega el circuito es cortar rápido y dejar rastro en el log.
     */
    private <T> T porElCircuito(Supplier<T> llamada) {
        try {
            return circuito.executeSupplier(llamada);
        } catch (CallNotPermittedException cortada) {
            log.warn("Circuito '{}' abierto: no se llama a Identity", circuito.getName());
            throw cortada;
        }
    }
}
