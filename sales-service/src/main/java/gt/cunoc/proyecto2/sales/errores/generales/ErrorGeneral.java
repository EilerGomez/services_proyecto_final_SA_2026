package gt.cunoc.proyecto2.sales.errores.generales;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

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