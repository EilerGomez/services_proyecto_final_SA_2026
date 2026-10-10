package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RG5/RS6: taller por categoría (mano de obra + repuestos). */
@Value
@Builder
public class TallerCategoriaFila {

    UUID categoriaId;
    String categoria;
    BigDecimal manoObra;
    BigDecimal repuestos;
    BigDecimal total;
}
