package gt.cunoc.proyecto2.sales.errores.historial;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

/** Errores del historial por NIT (S7, S8). */
public enum ErrorHistorial implements CodigoError {

    NIT_CONSUMIDOR_FINAL(TipoError.SOLICITUD_INVALIDA,
            "Las compras a consumidor final (CF) no se agrupan en un historial; indique un NIT");

    private final TipoError tipo;
    private final String mensaje;

    ErrorHistorial(TipoError tipo, String mensaje) {
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