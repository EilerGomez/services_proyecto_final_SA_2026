package gt.cunoc.proyecto2.identity.evento.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de TarifaDepreciacionCambiada.
 */
public record TarifaDepreciacionPayload(

        UUID tarifaId,
        BigDecimal montoPorKm,
        LocalDate vigenteDesde

) {
}
