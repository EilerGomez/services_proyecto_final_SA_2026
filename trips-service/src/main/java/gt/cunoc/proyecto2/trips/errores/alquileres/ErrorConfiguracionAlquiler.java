package gt.cunoc.proyecto2.trips.errores.alquileres;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

public enum ErrorConfiguracionAlquiler implements CodigoError {

    PRECIO_YA_EXISTE_PARA_FECHA(TipoError.CONFLICTO, "Ya hay un precio por km vigente desde el %s"),
    SIN_PRECIO_ALQUILER(TipoError.REGLA_NEGOCIO, "No hay un precio por km de alquiler vigente el %s");

    private final TipoError tipo;
    private final String mensaje;

    ErrorConfiguracionAlquiler(TipoError tipo, String mensaje) {
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