package gt.cunoc.proyecto2.fleet.errores.sucursales;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;
public enum ErrorSucursal implements CodigoError {

    SUCURSAL_NO_EXISTE(TipoError.NO_ENCONTRADO, "La sucursal no existe"),
    SUCURSAL_DUPLICADA(TipoError.CONFLICTO, "Ya existe una sucursal con el %s %s"),
    SUCURSAL_INACTIVA(TipoError.REGLA_NEGOCIO, "La sucursal está inactiva"),
    SUCURSAL_CON_BUSES_ACTIVOS(TipoError.REGLA_NEGOCIO,
            "No se puede desactivar: la sucursal tiene buses que no están inactivos");

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