package gt.cunoc.proyecto2.reporting.reportes;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Filtros comunes de los reportes. Fechas inclusivas en fecha de negocio. */
@Value
@Builder
public class ParametrosReporte {

    UUID sucursalId;
    UUID busId;
    UUID rutaId;
    UUID categoriaId;
    String tipo;
    String estado;
    LocalDate desde;
    LocalDate hasta;
    Integer limite;
}
