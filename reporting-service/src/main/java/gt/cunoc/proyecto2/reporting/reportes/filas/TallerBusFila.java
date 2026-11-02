package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Un gasto de taller de un bus (cada pago aparece por separado). */
@Value
@Builder
public class TallerBusFila {

    UUID busId;
    UUID categoriaId;
    String placa;
    String categoria;
    String fecha;
    String descripcion;
    String tipo;
    BigDecimal monto;
}
