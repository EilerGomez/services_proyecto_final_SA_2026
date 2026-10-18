package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/** RG5: un gasto de taller (detalle) para agrupar por categoría en el reporte. */
@Value
@Builder
public class TallerCategoriaFila {

    String categoria;
    String placa;
    String fecha;
    String descripcion;
    BigDecimal monto;
}
