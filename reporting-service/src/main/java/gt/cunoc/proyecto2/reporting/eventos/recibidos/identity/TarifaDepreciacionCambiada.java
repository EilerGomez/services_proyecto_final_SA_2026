package gt.cunoc.proyecto2.reporting.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tarifa de depreciación por kilómetro que Identity deja vigente desde una fecha.
 *
 * <p>Reporting solo guarda los campos que necesita; la tarifa se aplica a los viajes completados a
 * partir de ese momento.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TarifaDepreciacionCambiada(
        UUID tarifaId,
        BigDecimal montoPorKm,
        LocalDate vigenteDesde) {
}
