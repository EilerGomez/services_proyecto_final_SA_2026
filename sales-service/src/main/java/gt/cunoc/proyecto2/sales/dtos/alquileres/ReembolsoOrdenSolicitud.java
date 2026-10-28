package gt.cunoc.proyecto2.sales.dtos.alquileres;

import jakarta.validation.constraints.NotNull;

/**
 * Devolución en ventanilla del dinero de un alquiler cancelado. No lleva más datos: el monto sale de
 * la tarifa que ya se aplicó al cancelar, la fecha es la de hoy y quién entrega el efectivo sale del
 * token. La version evita que dos cajeros devuelvan lo mismo.
 */
public record ReembolsoOrdenSolicitud(@NotNull Integer version) {
}
