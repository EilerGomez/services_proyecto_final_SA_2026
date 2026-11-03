package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/* Se viola una regla de negocio (HTTP 422). */
public class ReglaNegocioException extends NegocioException {

    public ReglaNegocioException(String codigo, String mensaje) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensaje);
    }

    public ReglaNegocioException(String mensaje) {
        this("REGLA_NEGOCIO", mensaje);
    }
}
