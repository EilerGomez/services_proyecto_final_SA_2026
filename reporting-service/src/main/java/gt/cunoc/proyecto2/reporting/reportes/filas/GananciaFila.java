package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Ganancia por sucursal, en orden alfabético. */
@Value
@Builder
public class GananciaFila {

    UUID sucursalId;
    String sucursal;
    BigDecimal ingresos;
    BigDecimal gastos;
    BigDecimal ganancia;
}
