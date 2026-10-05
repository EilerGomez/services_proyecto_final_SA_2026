package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/** Otro microservicio no responde (fallback del circuit breaker) (HTTP 503). */
public class ServicioNoDisponibleException extends NegocioException {

    public ServicioNoDisponibleException(String codigo, String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, codigo, mensaje);
    }

    public ServicioNoDisponibleException(String mensaje) {
        this("SERVICIO_NO_DISPONIBLE", mensaje);
    }
}
