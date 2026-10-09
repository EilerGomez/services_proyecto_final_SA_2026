package gt.cunoc.proyecto2.trips.errores.generales;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

/** Errores que no son de una tabla en particular. */
public enum ErrorGeneral implements CodigoError {

    MODIFICACION_CONCURRENTE(TipoError.CONFLICTO,
            "Otro usuario modificó el registro; vuelva a cargarlo e intente de nuevo");

    private final TipoError tipo;
    private final String mensaje;

    ErrorGeneral(TipoError tipo, String mensaje) {
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