package gt.cunoc.proyecto2.trips.modelos.alquileres;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de un alquiler (columna alquileres_privados.estado):
 * SOLICITADO → COTIZADO → PAGADO → ASIGNADO → COMPLETADO.
 * Cualquiera de esos cuatro primeros → CANCELADO, con el reembolso que indique la tarifa vigente
 * (tabla tarifas_reembolso de Sales). Un alquiler COMPLETADO ya no se cancela: el viaje se hizo.
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
    /** Cancelado (T21). Si ya había pago, Sales devuelve el porcentaje de la tarifa que aplique. */
    CANCELADO;

    /**
     * Estados desde los que se puede cancelar. Si ya se pagó, la tarifa de reembolso decide además si
     * la cancelación se permite a esa hora y cuánto se devuelve.
     */
    public static final Set<EstadoAlquiler> CANCELABLES =
            EnumSet.of(SOLICITADO, COTIZADO, PAGADO, ASIGNADO);

    /** Desde PAGADO ya entró dinero: cancelar implica devolver algo. */
    public boolean conPagoHecho() {
        return this == PAGADO || this == ASIGNADO;
    }
}