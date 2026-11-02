package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Ingreso por boleto de la sucursal. */
@Value
@Builder
public class BoletoFila {

    String fecha;
    UUID ventaId;
    String ruta;
    String busPlaca;
    String clienteNit;
    String movimiento;
    Integer boletos;
    BigDecimal monto;
}
