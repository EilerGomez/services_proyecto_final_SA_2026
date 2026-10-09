package gt.cunoc.proyecto2.trips.errores.rutas;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

/** Errores de rutas (T3–T7). */
public enum ErrorRuta implements CodigoError {

    RUTA_NO_EXISTE(TipoError.NO_ENCONTRADO, "La ruta no existe"),
    RUTA_DUPLICADA(TipoError.CONFLICTO, "Ya existe una ruta de %s a %s"),
    MISMA_SUCURSAL(TipoError.REGLA_NEGOCIO, "El origen y el destino deben ser sucursales distintas"),
    RUTA_CON_VIAJES(TipoError.CONFLICTO,
            "La ruta tiene viajes y no se puede eliminar; desactívela para que no se programen más");

    private final TipoError tipo;
    private final String mensaje;

    ErrorRuta(TipoError tipo, String mensaje) {
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