package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Viaje completado con su depreciación. */
@Value
@Builder
public class DepreciacionFila {

    UUID busId;
    String placa;
    UUID viajeId;
    String fecha;
    BigDecimal kmRecorridos;
    BigDecimal tarifa;
    BigDecimal costo;
}
