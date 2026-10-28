package gt.cunoc.proyecto2.trips.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de TarifaReembolsoCambiada (de Sales). Mantiene la copia local de la política de
 * cancelación: Trips solo decide con ella si una cancelación se permite.
 * estadoAlquiler puede ser CUALQUIERA (comodín), que no es un estado del alquiler.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TarifaReembolsoCambiada(
        UUID tarifaId,
        String estadoAlquiler,
        int horasMinimasAntes,
        boolean permiteCancelacion,
        BigDecimal porcentajeReembolso,
        String justificacion,
        boolean activa) {
}
