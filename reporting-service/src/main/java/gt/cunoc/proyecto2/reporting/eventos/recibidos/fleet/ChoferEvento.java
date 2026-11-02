package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de un chofer en los eventos de creación y actualización de Fleet.
 *
 * <p>Incluye su licencia y vencimiento, que alimentan el reporte de choferes y los avisos de
 * vencimiento.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferEvento(
        UUID choferId,
        UUID usuarioId,
        UUID sucursalId,
        String nombreCompleto,
        String numeroLicencia,
        String tipoLicencia,
        LocalDate fechaVencimientoLicencia,
        BigDecimal salarioBaseViaje,
        String estado) {
}
