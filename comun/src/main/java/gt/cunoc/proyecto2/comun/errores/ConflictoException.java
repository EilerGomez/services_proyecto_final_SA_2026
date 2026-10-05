package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/** Conflicto con el estado actual (p. ej. asiento ocupado) (HTTP 409). */
public class ConflictoException extends NegocioException {

    public ConflictoException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public ConflictoException(String mensaje) {
        this("CONFLICTO", mensaje);
    }
}
