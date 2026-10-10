package gt.cunoc.proyecto2.sales.modelos.ventas;

/*
 * PENDIENTE_PAGO → CONFIRMADA | RECHAZADA · CONFIRMADA → CANCELADA → REEMBOLSADA · RECHAZADA → REEMBOLSADA.
 * La venta en mostrador nace CONFIRMADA.
 */
public enum EstadoVenta {
    PENDIENTE_PAGO,
    CONFIRMADA,
    RECHAZADA,
    CANCELADA,
    REEMBOLSADA
}