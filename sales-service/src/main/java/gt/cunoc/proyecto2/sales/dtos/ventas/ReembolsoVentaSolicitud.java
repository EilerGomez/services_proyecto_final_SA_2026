package gt.cunoc.proyecto2.sales.dtos.ventas;

import jakarta.validation.constraints.NotNull;

/**
 * S14 · Devolución del dinero de una venta cancelada. No lleva más datos: la fecha es la de hoy y
 * quién entrega el efectivo sale del token. La version evita que dos cajeros reembolsen lo mismo.
 */
public record ReembolsoVentaSolicitud(@NotNull Integer version) {
}
