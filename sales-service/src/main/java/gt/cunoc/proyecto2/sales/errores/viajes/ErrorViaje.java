package gt.cunoc.proyecto2.sales.errores.viajes;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

public enum ErrorViaje implements CodigoError {

    VIAJE_NO_DISPONIBLE(TipoError.NO_ENCONTRADO, "El viaje no existe o ya no está a la venta");

    private final TipoError tipo;
    private final String mensaje;

    ErrorViaje(TipoError tipo, String mensaje) {
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