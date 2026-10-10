package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/** RG2: gasto agregado por sucursal y tipo. */
@Value
@Builder
public class GastoGlobalFila {

    String sucursal;
    String tipo;
    BigDecimal monto;
}
