package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RS2: chofer de la sucursal. */
@Value
@Builder
public class ChoferFila {

    UUID choferId;
    String nombreCompleto;
    String numeroLicencia;
    String tipoLicencia;
    String fechaVencimientoLicencia;
    String estado;
    Integer totalViajes;
}
