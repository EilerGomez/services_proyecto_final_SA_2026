package gt.cunoc.proyecto2.comun.eventos.contextocorrelacion;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Id de correlación del flujo actual (una petición HTTP o el procesamiento de un evento).
 * Viaja en el header {@value #HEADER} y en el campo correlationId de los eventos,
 * así se puede seguir una saga completa en los logs de todos los servicios.
 */
public final class ContextoCorrelacion {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CLAVE_MDC = "correlationId";

    private static final ThreadLocal<UUID> ACTUAL = new ThreadLocal<>();

    private ContextoCorrelacion() {
    }

    public static Optional<UUID> actual() {
        return Optional.ofNullable(ACTUAL.get());
    }

    public static UUID actualOGenerar() {
        UUID id = ACTUAL.get();
        return id != null ? id : UUID.randomUUID();
    }

    public static void establecer(UUID id) {
        ACTUAL.set(id);
        MDC.put(CLAVE_MDC, id.toString());
    }

    public static void limpiar() {
        ACTUAL.remove();
        MDC.remove(CLAVE_MDC);
    }
}
