package gt.cunoc.proyecto2.sales.errores.tarifas;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

/** Errores del CRUD de tarifas de reembolso. */
public enum ErrorTarifaReembolso implements CodigoError {

    TARIFA_NO_EXISTE(TipoError.NO_ENCONTRADO, "La tarifa de reembolso no existe"),
    TRAMO_DUPLICADO(TipoError.CONFLICTO,
            "Ya hay una tarifa para alquileres %s con %d horas de anticipación: edite esa fila");

    private final TipoError tipo;
    private final String mensaje;

    ErrorTarifaReembolso(TipoError tipo, String mensaje) {
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
