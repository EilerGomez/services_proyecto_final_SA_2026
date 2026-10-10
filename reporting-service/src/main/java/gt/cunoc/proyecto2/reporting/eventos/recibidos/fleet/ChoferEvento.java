package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Payload de ChoferCreado y ChoferActualizado (mismo contenido). */
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
