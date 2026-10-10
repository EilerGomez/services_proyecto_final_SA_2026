package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RS6: taller por bus y categoría. */
@Value
@Builder
public class TallerBusFila {

    UUID busId;
    String placa;
    String categoria;
    BigDecimal manoObra;
    BigDecimal repuestos;
    BigDecimal total;
}
