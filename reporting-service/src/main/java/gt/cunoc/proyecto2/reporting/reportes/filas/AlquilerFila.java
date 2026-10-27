package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RS4: ingreso por alquiler de la sucursal. */
@Value
@Builder
public class AlquilerFila {

    String fecha;
    String fechaSalida;
    String fechaRetorno;
    UUID ordenId;
    String clienteNit;
    String clienteNombre;
    String origen;
    String destino;
    /** Placa del bus asignado al alquiler; "-" mientras no tenga viaje asignado. */
    String busPlaca;
    BigDecimal monto;
}
