package gt.cunoc.proyecto2.sales.eventos;

/**
 * Motivos que escribe Sales en un rechazo (motivo_rechazo) o en un ReembolsoCarteraSolicitado.
 * Los rechazos de Identity traen su propio motivo (SALDO_INSUFICIENTE, USUARIO_INACTIVO, CARTERA_INEXISTENTE).
 */
public enum MotivoSaga {
    /** La reserva de 5 minutos venció (tarea programada) o la venta perdió algún asiento. */
    TIEMPO_AGOTADO,
    /** Trips eliminó el viaje: ventas pendientes rechazadas y confirmadas canceladas con reembolso. */
    VIAJE_ELIMINADO,
    /** El viaje ya salió o ya no existe cuando llegó el débito. */
    VIAJE_NO_DISPONIBLE,
    /** Identity cobró después de que la venta u orden ya no podía pagarse: se devuelve el dinero. */
    DEBITO_TARDIO,
    /** S5: un administrador canceló una venta pagada con cartera. */
    VENTA_CANCELADA
}
