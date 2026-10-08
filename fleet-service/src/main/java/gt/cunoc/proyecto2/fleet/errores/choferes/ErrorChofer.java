package gt.cunoc.proyecto2.fleet.errores.choferes;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

public enum ErrorChofer implements CodigoError {

    CHOFER_NO_EXISTE(TipoError.NO_ENCONTRADO, "El chofer no existe"),
    LICENCIA_EN_USO(TipoError.CONFLICTO, "Ya existe un chofer con la licencia %s"),
    LICENCIA_VENCIDA(TipoError.REGLA_NEGOCIO, "La licencia vence el %s, antes de la fecha de registro %s"),
    CHOFER_INACTIVO(TipoError.REGLA_NEGOCIO, "El chofer está inactivo"),
    CHOFER_CON_VIAJES_ACTIVOS(TipoError.REGLA_NEGOCIO,
            "No se puede desactivar: el chofer tiene %d viaje(s) programado(s) o en ruta");

    private final TipoError tipo;
    private final String mensaje;

    ErrorChofer(TipoError tipo, String mensaje) {
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