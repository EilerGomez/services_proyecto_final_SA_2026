package gt.cunoc.proyecto2.sales.eventos;

/**
 * Qué se cobra o se devuelve en la cartera (campo referenciaTipo de la saga con Wallet).
 * referenciaId es el ventaId (VENTA) o el ordenId (ALQUILER). Wallet no repite un débito
 * ni un reembolso de la misma referencia.
 */
public enum ReferenciaCartera {
    VENTA,
    ALQUILER
}
