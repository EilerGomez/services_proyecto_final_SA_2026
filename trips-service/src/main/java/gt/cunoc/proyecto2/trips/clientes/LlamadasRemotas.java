package gt.cunoc.proyecto2.trips.clientes;

import gt.cunoc.proyecto2.comun.errores.ErrorRespuesta;
import gt.cunoc.proyecto2.trips.errores.CodigoError;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Forma única de hacer una llamada REST a otro servicio (la usan ClienteFleet):
 * circuito + reintentos de red + traducción de errores.
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

    public static <T> Optional<T> ejecutarOpcional(CircuitBreaker circuito, Supplier<T> llamada,
                                                   CodigoError siNoResponde, Set<String> codigosNoExiste) {
        return ejecutar(circuito, () -> {
            try {
                T respuesta = llamada.get();
                return respuesta == null ? null : Optional.of(respuesta);   // null → 503 en ejecutar
            } catch (HttpClientErrorException.NotFound e) {
                String codigo = codigoDe(e);
                if (codigo != null && codigosNoExiste.contains(codigo)) {   // Set.of no acepta contains(null)
                    return Optional.<T>empty();
                }
                throw e;
            }
        }, siNoResponde);
    }

    private static String codigoDe(HttpClientErrorException e) {
        try {
            ErrorRespuesta cuerpo = e.getResponseBodyAs(ErrorRespuesta.class);
            return cuerpo == null ? null : cuerpo.codigo();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}