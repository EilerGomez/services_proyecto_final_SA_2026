package gt.cunoc.proyecto2.trips.errores.integraciones;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

public enum ErrorIntegracion implements CodigoError {

    FLEET_NO_DISPONIBLE(TipoError.SERVICIO_NO_DISPONIBLE,
            "No se pudo validar el bus o el chofer con Flota; intente de nuevo en unos minutos");

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