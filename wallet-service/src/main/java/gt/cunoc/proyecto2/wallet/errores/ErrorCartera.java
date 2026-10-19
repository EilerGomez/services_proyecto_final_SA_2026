package gt.cunoc.proyecto2.wallet.errores;

/** Códigos de error del servicio de carteras. */
public enum ErrorCartera implements CodigoError {

    CARTERA_NO_EXISTE(TipoError.NO_ENCONTRADO, "El usuario no tiene cartera"),
    MONTO_RECARGA_INVALIDO(TipoError.SOLICITUD_INVALIDA,
            "El monto de la recarga debe ser mayor que cero y tener como máximo dos decimales"),
    PAGO_RECHAZADO(TipoError.REGLA_NEGOCIO, "%s"),
    SALDO_NEGATIVO(TipoError.REGLA_NEGOCIO, "La operación dejaría la cartera con saldo negativo"),
    SALDO_EXCEDE_LIMITE(TipoError.REGLA_NEGOCIO, "La cartera no puede superar un saldo de Q%s"),
    MONTO_INVALIDO(TipoError.REGLA_NEGOCIO, "El monto del reembolso debe ser mayor que cero"),
    TIPO_INVALIDO(TipoError.SOLICITUD_INVALIDA, "El tipo debe ser COBRO o DEVOLUCION"),
    SALDO_SISTEMA_INSUFICIENTE(TipoError.REGLA_NEGOCIO, "La billetera del sistema no tiene saldo para el reembolso"),
    IDENTITY_NO_DISPONIBLE(TipoError.SERVICIO_NO_DISPONIBLE,
            "No se pudo verificar el estado de la cuenta, intente de nuevo");

    /** Mensaje cuando la pasarela rechaza el cobro sin decir por qué. */
    public static final String PAGO_RECHAZADO_SIN_MOTIVO = "La pasarela de pagos rechazó el cobro";

    private final TipoError tipo;
    private final String mensaje;

    ErrorCartera(TipoError tipo, String mensaje) {
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
