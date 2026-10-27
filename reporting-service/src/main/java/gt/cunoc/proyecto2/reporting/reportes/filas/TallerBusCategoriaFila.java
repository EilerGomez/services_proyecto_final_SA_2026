package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * RS6 agrupado: lo que un bus lleva gastado en una categoría. El monto ya es la suma de todos sus
 * gastos de esa categoría, y "gastos" dice cuántos fueron: dos números juntos que delatan al bus
 * que vuelve y vuelve al taller por lo mismo.
 */
@Value
@Builder
public class TallerBusCategoriaFila {

    UUID busId;
    UUID categoriaId;
    String placa;
    String categoria;
    int gastos;
    BigDecimal monto;
}
