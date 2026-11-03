package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/* Falta el token o no es válido (HTTP 401). */
public class NoAutenticadoException extends NegocioException {

    public NoAutenticadoException(String codigo, String mensaje) {
        super(HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }

    public NoAutenticadoException(String mensaje) {
        this("NO_AUTENTICADO", mensaje);
    }
}
