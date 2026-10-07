package gt.cunoc.proyecto2.fleet.errores.bitacora;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

public enum ErrorBitacora implements CodigoError {

    RANGO_FECHAS_INVALIDO(TipoError.SOLICITUD_INVALIDA, "La fecha desde (%s) es posterior a la fecha hasta (%s)"),
    KILOMETRAJE_INVALIDO(TipoError.REGLA_NEGOCIO, "El kilometraje final (%s) es menor que el inicial (%s)");

    private final TipoError tipo;
    private final String mensaje;

    ErrorBitacora(TipoError tipo, String mensaje) {
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