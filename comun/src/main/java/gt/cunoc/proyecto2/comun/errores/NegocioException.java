package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de todos los errores esperados del negocio.
 * El manejador global la convierte en una {@link ErrorRespuesta} con el status y código indicados.
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
