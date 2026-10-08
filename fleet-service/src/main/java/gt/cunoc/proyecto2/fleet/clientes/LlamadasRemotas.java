package gt.cunoc.proyecto2.fleet.clientes;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Forma única de hacer una llamada REST a otro servicio (la usan ClienteIdentity y ClienteTrips):
 * circuito + reintentos de red + traducción de errores.
 *
 * <ul>
 *   <li>400/409/422 del otro servicio: llegan al usuario con el mismo código.</li>
 *   <li>Sin respuesta, 5xx o circuito abierto: 503 con el error indicado en siNoResponde.</li>
 * </ul>
 */
public final class LlamadasRemotas {

    private static final Logger log = LoggerFactory.getLogger(LlamadasRemotas.class);

    private LlamadasRemotas() {
    }

    public static <T> T ejecutar(CircuitBreaker circuito, Supplier<T> llamada, CodigoError siNoResponde) {
        try {
            T respuesta = circuito.executeSupplier(() -> Reintentos.ejecutar(llamada));
            if (respuesta == null) {
                log.error("El servicio '{}' respondió sin cuerpo", circuito.getName());
                throw siNoResponde.excepcion();
            }
            return respuesta;
        } catch (HttpClientErrorException e) {
            throw TraductorErroresRemotos.traducir(e, siNoResponde);
        } catch (CallNotPermittedException | RestClientException e) {
            log.warn("Llamada al servicio '{}' falló: {}", circuito.getName(), e.getMessage());
            throw siNoResponde.excepcion();
        }
    }
}