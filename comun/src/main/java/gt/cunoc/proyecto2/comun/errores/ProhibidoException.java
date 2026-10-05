package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/** El usuario no tiene permiso para la operación (HTTP 403). */
public class ProhibidoException extends NegocioException {

    public ProhibidoException(String codigo, String mensaje) {
        super(HttpStatus.FORBIDDEN, codigo, mensaje);
    }

    public ProhibidoException(String mensaje) {
        this("PROHIBIDO", mensaje);
    }
}
