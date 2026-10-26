package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RG1: una fila de ingreso (las cancelaciones llevan monto negativo). */
@Value
@Builder
public class IngresoGlobalFila {

    String fecha;
    String sucursal;
    String tipo;
    String movimiento;
    UUID referenciaId;
    String detalle;
    String clienteNit;
    Integer boletos;
    BigDecimal monto;
}
