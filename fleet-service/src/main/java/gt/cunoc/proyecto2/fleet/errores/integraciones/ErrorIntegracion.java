package gt.cunoc.proyecto2.fleet.errores.integraciones;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

public enum ErrorIntegracion implements CodigoError {

    IDENTITY_NO_DISPONIBLE(TipoError.SERVICIO_NO_DISPONIBLE,
            "El servicio de cuentas no está disponible; intente de nuevo en unos minutos"),
    TRIPS_NO_DISPONIBLE(TipoError.SERVICIO_NO_DISPONIBLE,
            "No se pudieron consultar los viajes; intente de nuevo en unos minutos");

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