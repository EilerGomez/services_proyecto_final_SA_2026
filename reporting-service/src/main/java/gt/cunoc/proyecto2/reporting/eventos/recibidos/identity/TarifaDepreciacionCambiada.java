package gt.cunoc.proyecto2.reporting.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Solo los campos de TarifaDepreciacionCambiada que usa Reporting. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TarifaDepreciacionCambiada(
        UUID tarifaId,
        BigDecimal montoPorKm,
        LocalDate vigenteDesde) {
}
