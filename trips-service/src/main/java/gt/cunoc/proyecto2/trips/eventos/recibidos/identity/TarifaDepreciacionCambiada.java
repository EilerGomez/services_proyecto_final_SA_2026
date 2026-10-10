package gt.cunoc.proyecto2.trips.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Payload de TarifaDepreciacionCambiada (de Identity). Las tarifas no se editan: cada una es nueva. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TarifaDepreciacionCambiada(
        UUID tarifaId,
        BigDecimal montoPorKm,
        LocalDate vigenteDesde) {
}