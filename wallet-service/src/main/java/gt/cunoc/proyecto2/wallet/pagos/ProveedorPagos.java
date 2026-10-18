package gt.cunoc.proyecto2.wallet.pagos;

/**
 * Puerto hacia la pasarela de pagos con la que se cobran las recargas.
 *
 * <p>La cartera no sabe con quién cobra: solo usa esta interfaz. Conectar una pasarela real
 * (Stripe, PayPal, una pasarela local, ...) es agregar una clase que la implemente y elegirla con
 * {@code app.pagos.proveedor}; no se toca el resto del servicio ni los demás microservicios, que solo
 * ven los eventos de la cartera.
 *
 * <p>Pasarelas con confirmación asíncrona (pago en otra página y aviso por webhook): {@link #cobrar}
 * devuelve el resultado pendiente y el webhook llama a la acreditación con la misma referencia externa;
 * la restricción {@code UNIQUE(proveedor_pago, referencia_externa)} evita acreditar dos veces.
 */
public interface ProveedorPagos {

    /** Código que se guarda en cada recarga (p. ej. "SIMULADO", "STRIPE"). */
    String codigo();

    /** Cobra el monto al usuario. No lanza excepción por un rechazo: lo informa en el resultado. */
    ResultadoCobro cobrar(SolicitudCobro solicitud);
}
