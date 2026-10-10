package gt.cunoc.proyecto2.sales.errores.ventas;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

public enum ErrorVenta implements CodigoError {

    VENTA_NO_EXISTE(TipoError.NO_ENCONTRADO, "La venta no existe"),
    VENTA_AJENA(TipoError.PROHIBIDO,
            "Una venta solo la ven su cliente y el personal de la sucursal que la atendió"),
    OTRA_SUCURSAL(TipoError.PROHIBIDO, "Solo puede vender boletos de viajes que salen de su sucursal"),
    ASIENTOS_REPETIDOS(TipoError.SOLICITUD_INVALIDA, "Hay asientos repetidos en la solicitud"),
    ASIENTO_FUERA_DE_RANGO(TipoError.SOLICITUD_INVALIDA, "El bus tiene asientos del 1 al %d; el %d no existe"),
    ASIENTO_OCUPADO(TipoError.CONFLICTO, "Alguno de los asientos %s ya no está disponible; elija otros"),
    RANGO_FECHAS_INVALIDO(TipoError.SOLICITUD_INVALIDA, "La fecha inicial (%s) es posterior a la final (%s)");

    private final TipoError tipo;
    private final String mensaje;

    ErrorVenta(TipoError tipo, String mensaje) {
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