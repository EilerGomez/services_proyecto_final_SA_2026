package gt.cunoc.proyecto2.sales.errores.integraciones;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

/* Errores de las llamadas REST a otros servicios (R2 a Trips). */
public enum ErrorIntegracion implements CodigoError {

    TRIPS_NO_DISPONIBLE(TipoError.SERVICIO_NO_DISPONIBLE,
            "No se pudo confirmar el viaje con Viajes; intente de nuevo en unos minutos");

    private final TipoError tipo;
    private final String mensaje;

    ErrorIntegracion(TipoError tipo, String mensaje) {
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