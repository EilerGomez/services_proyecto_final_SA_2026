package gt.cunoc.proyecto2.trips.errores.sucursales;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

public enum ErrorSucursal implements CodigoError {

    SUCURSAL_NO_EXISTE(TipoError.REGLA_NEGOCIO, "La sucursal %s no existe"),
    SUCURSAL_INACTIVA(TipoError.REGLA_NEGOCIO, "La sucursal %s está inactiva");

    private final TipoError tipo;
    private final String mensaje;

    ErrorSucursal(TipoError tipo, String mensaje) {
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