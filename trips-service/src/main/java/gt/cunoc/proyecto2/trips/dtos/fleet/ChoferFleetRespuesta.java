package gt.cunoc.proyecto2.trips.dtos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoferFleetRespuesta(
        UUID choferId,
        UUID sucursalId,
        UUID usuarioId,
        String nombreCompleto,
        String estado,
        BigDecimal salarioBaseViaje,
        LocalDate fechaVencimientoLicencia,
        boolean licenciaVigente) {

    private static final String ACTIVO = "ACTIVO";

    public boolean esActivo() {
        return ACTIVO.equals(estado);
    }
}