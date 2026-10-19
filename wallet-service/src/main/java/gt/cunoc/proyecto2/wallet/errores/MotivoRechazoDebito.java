package gt.cunoc.proyecto2.wallet.errores;

/** Motivo que viaja en el evento DebitoCarteraRechazado (se publica con {@code name()}). */
public enum MotivoRechazoDebito {
    SALDO_INSUFICIENTE,
    USUARIO_INACTIVO,
    CARTERA_INEXISTENTE
}
