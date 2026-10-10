package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RS1: bus de la sucursal. */
@Value
@Builder
public class BusFila {

    UUID busId;
    String placa;
    String marca;
    String modelo;
    Integer capacidadPasajeros;
    String estado;
    String choferActual;
    BigDecimal kilometrajeActual;
    Integer totalViajes;
}
