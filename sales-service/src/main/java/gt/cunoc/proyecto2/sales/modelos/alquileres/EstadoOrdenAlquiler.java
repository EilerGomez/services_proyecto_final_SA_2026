package gt.cunoc.proyecto2.sales.modelos.alquileres;

/*
 * PENDIENTE_PAGO → PAGADA | RECHAZADA · PENDIENTE_PAGO/RECHAZADA → REEMBOLSADA (débito tardío)
 * PAGADA → CANCELADA → REEMBOLSADA (se canceló el alquiler y se devolvió el porcentaje que toca).
 */
public enum EstadoOrdenAlquiler {
    PENDIENTE_PAGO,
    PAGADA,
    RECHAZADA,
    /** Se canceló el alquiler. Si había dinero cobrado, queda pendiente de devolver. */
    CANCELADA,
    REEMBOLSADA
}
