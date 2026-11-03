package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/* El recurso no existe (HTTP 404). */
public class NoEncontradoException extends NegocioException {

    public NoEncontradoException(String codigo, String mensaje) {
        super(HttpStatus.NOT_FOUND, codigo, mensaje);
    }

    public NoEncontradoException(String mensaje) {
        this("NO_ENCONTRADO", mensaje);
    }
}
