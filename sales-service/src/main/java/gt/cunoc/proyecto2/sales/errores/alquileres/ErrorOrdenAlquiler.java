package gt.cunoc.proyecto2.sales.errores.alquileres;

import gt.cunoc.proyecto2.sales.errores.CodigoError;
import gt.cunoc.proyecto2.sales.errores.TipoError;

public enum ErrorOrdenAlquiler implements CodigoError {

    ORDEN_NO_EXISTE(TipoError.NO_ENCONTRADO, "La orden de pago no existe"),
    ORDEN_AJENA(TipoError.PROHIBIDO,
            "Una orden de pago solo la ven su cliente y el personal de la sucursal del alquiler"),
    ESTADO_INVALIDO(TipoError.REGLA_NEGOCIO,
            "La orden está %s; solo se paga si está PENDIENTE_PAGO o RECHAZADA"),
    ALQUILER_SIN_CUENTA(TipoError.REGLA_NEGOCIO,
            "El alquiler se pidió en mostrador sin cuenta: solo se puede pagar en efectivo"),
    PAGO_EN_PROCESO(TipoError.CONFLICTO,
            "Ya se está cobrando esta orden con la cartera; espere el resultado");

    private final TipoError tipo;
    private final String mensaje;

    ErrorOrdenAlquiler(TipoError tipo, String mensaje) {
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