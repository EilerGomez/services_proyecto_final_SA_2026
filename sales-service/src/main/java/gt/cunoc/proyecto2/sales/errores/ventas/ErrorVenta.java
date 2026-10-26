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
    RANGO_FECHAS_INVALIDO(TipoError.SOLICITUD_INVALIDA, "La fecha inicial (%s) es posterior a la final (%s)"),
    CLIENTE_SIN_PERFIL(TipoError.REGLA_NEGOCIO,
            "Su perfil de cliente aún no está disponible para compras; intente de nuevo en unos segundos"),
    ESTADO_INVALIDO(TipoError.REGLA_NEGOCIO, "La venta está %s; esta acción solo aplica a ventas %s"),
    VIAJE_YA_INICIO(TipoError.REGLA_NEGOCIO, "El viaje ya salió o ya no existe; la venta no se puede cancelar"),
    FECHA_CANCELACION_INVALIDA(TipoError.SOLICITUD_INVALIDA,
            "La fecha de cancelación (%s) no puede ser anterior a la fecha de pago (%s)"),
    VENTA_YA_REEMBOLSADA(TipoError.REGLA_NEGOCIO, "Esta venta ya fue reembolsada"),
    REEMBOLSO_NO_EN_EFECTIVO(TipoError.REGLA_NEGOCIO,
            "La venta se pagó con la cartera digital: el reembolso lo hace la cartera, no la sucursal"),
    CODIGO_INVALIDO(TipoError.SOLICITUD_INVALIDA,
            "El código del boleto debe tener al menos 12 caracteres; revise el código o escanéelo de nuevo");

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