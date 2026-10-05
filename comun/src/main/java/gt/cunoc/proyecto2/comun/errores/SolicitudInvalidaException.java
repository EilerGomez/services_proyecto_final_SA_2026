package gt.cunoc.proyecto2.comun.errores;

import org.springframework.http.HttpStatus;

/** Datos de entrada inválidos (HTTP 400). */
public class SolicitudInvalidaException extends NegocioException {

    public SolicitudInvalidaException(String codigo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }

    public SolicitudInvalidaException(String mensaje) {
        this("SOLICITUD_INVALIDA", mensaje);
    }
}
