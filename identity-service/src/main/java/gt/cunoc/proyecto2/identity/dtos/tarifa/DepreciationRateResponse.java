package gt.cunoc.proyecto2.identity.dtos.tarifa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de una tarifa de depreciación (I15, I16).
 */
public record DepreciationRateResponse(

        UUID tarifaId,
        BigDecimal montoPorKm,
        LocalDate vigenteDesde,
        UUID creadoPor,
        Instant fechaCreacion

) {
}
