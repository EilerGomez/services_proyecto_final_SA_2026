package gt.cunoc.proyecto2.trips.errores.operaciones;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

/** Errores de la salida (T14) y la llegada (T15) de un viaje. */
public enum ErrorOperacionViaje implements CodigoError {

    CHOFER_NO_ASIGNADO(TipoError.PROHIBIDO, "Solo el chofer asignado o un administrador de la sucursal registra la salida o la llegada"),
    SALIDA_YA_REGISTRADA(TipoError.CONFLICTO, "La salida de este viaje ya fue registrada"),
    VIAJE_NO_INICIADO(TipoError.REGLA_NEGOCIO, "El viaje no ha salido; registre primero la salida"),
    LLEGADA_YA_REGISTRADA(TipoError.CONFLICTO, "La llegada de este viaje ya fue registrada"),
    LLEGADA_ANTES_DE_SALIDA(TipoError.SOLICITUD_INVALIDA, "La llegada debe ser posterior a la salida (%s)"),
    KM_INVALIDO(TipoError.REGLA_NEGOCIO, "El kilometraje final (%s) es menor que el inicial (%s)"),
    SIN_TARIFA_VIGENTE(TipoError.REGLA_NEGOCIO, "No hay una tarifa de depreciación vigente el %s");

    private final TipoError tipo;
    private final String mensaje;

    ErrorOperacionViaje(TipoError tipo, String mensaje) {
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    @Override
    public TipoError tipo() {
        return tipo;
    }

    @Override
    public String mensaje() {
        return mensaje;
    }
}