package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Lo que un bus lleva gastado en una categoría de taller.
 *
 * <p>El monto ya es la suma de todos sus gastos de esa categoría y {@code gastos} dice cuántos
 * fueron; con ambos se detecta un bus que vuelve al taller por lo mismo.
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
