package gt.cunoc.proyecto2.identity.dtos.tarifa;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


/**
 * Vista de una tarifa de depreciación.
 */
public record DepreciationRateResponse(


        UUID id,


        BigDecimal montoPorKm,


        LocalDate vigenteDesde

) {
}
