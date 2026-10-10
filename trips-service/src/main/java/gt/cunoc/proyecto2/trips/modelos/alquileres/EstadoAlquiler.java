package gt.cunoc.proyecto2.trips.modelos.alquileres;

/**
 * Estados de un alquiler (columna alquileres_privados.estado):
 * SOLICITADO → COTIZADO → PAGADO → ASIGNADO → COMPLETADO; SOLICITADO → CANCELADO.
 */
public enum EstadoAlquiler {
    /** Pedido por el cliente con un precio estimado (T16). */
    SOLICITADO,
    /** El admin confirmó el precio (T20); Sales crea la orden de pago. */
    COTIZADO,
    /** Sales avisó el pago (evento AlquilerPagado). */
    PAGADO,
    /** Tiene un viaje PRIVADO con bus y chofer (T22). */
    ASIGNADO,
    /** Su viaje llegó (T15). */
    COMPLETADO,
    /** Cancelado antes de cotizar (T21). */
    CANCELADO
}