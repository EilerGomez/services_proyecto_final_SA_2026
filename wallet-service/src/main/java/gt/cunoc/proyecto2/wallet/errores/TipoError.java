package gt.cunoc.proyecto2.wallet.errores;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;

import java.util.function.BiFunction;

/** Tipo de error de negocio: decide la excepción (y con ella el estado HTTP) que se lanza. */
public enum TipoError {

    SOLICITUD_INVALIDA(SolicitudInvalidaException::new),        // 400
    PROHIBIDO(ProhibidoException::new),                          // 403
    NO_ENCONTRADO(NoEncontradoException::new),                   // 404
    CONFLICTO(ConflictoException::new),                          // 409
    REGLA_NEGOCIO(ReglaNegocioException::new),                   // 422
    SERVICIO_NO_DISPONIBLE(ServicioNoDisponibleException::new);  // 503

    private final BiFunction<String, String, NegocioException> fabrica;

    TipoError(BiFunction<String, String, NegocioException> fabrica) {
        this.fabrica = fabrica;
    }

    NegocioException crear(String codigo, String mensaje) {
        return fabrica.apply(codigo, mensaje);
    }
}
