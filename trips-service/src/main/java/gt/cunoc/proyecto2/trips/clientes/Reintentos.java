package gt.cunoc.proyecto2.trips.clientes;

import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.web.client.ResourceAccessException;

/*
 * Reintenta una llamada REST solo cuando falla la red (no conecta o pasan los 3 s de espera).
 * Las respuestas 4xx y 5xx no se reintentan aquí.
 * Solo se usa con llamadas seguras de repetir (los GET de R1).
 */
public final class Reintentos {

    public static final int INTENTOS = 3;                         // 1 intento + 2 reintentos
    public static final Duration ESPERA = Duration.ofMillis(300);

    private Reintentos() {
    }

    public static <T> T ejecutar(Supplier<T> llamada) {
        for (int intento = 1; ; intento++) {
            try {
                return llamada.get();
            } catch (ResourceAccessException e) {
                if (intento >= INTENTOS) {
                    throw e;
                }
                esperar();
            }
        }
    }

    private static void esperar() {
        try {
            Thread.sleep(ESPERA.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}