package gt.cunoc.proyecto2.fleet.errores.buses;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

/** Errores de buses (F10–F14, R1a). */
public enum ErrorBus implements CodigoError {

    BUS_NO_EXISTE(TipoError.NO_ENCONTRADO, "El bus no existe"),
    PLACA_EN_USO(TipoError.CONFLICTO, "Ya existe un bus con la placa %s"),
    ESTADO_NO_PERMITIDO(TipoError.SOLICITUD_INVALIDA, "El estado %s no se asigna manualmente"),
    BUS_EN_RUTA(TipoError.REGLA_NEGOCIO, "No se puede cambiar el estado de un bus que está en ruta"),
    BUS_CON_VIAJES_ACTIVOS(TipoError.REGLA_NEGOCIO,
            "No se puede desactivar: el bus tiene %d viaje(s) programado(s) o en ruta");

    private final TipoError tipo;
    private final String mensaje;

    ErrorBus(TipoError tipo, String mensaje) {
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