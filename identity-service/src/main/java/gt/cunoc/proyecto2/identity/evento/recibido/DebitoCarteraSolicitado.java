package gt.cunoc.proyecto2.identity.evento.recibido;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


/**
 * Solicitud de débito de cartera publicada por Sales.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebitoCarteraSolicitado(


        String referenciaTipo,


        UUID referenciaId,


        UUID usuarioId,


        BigDecimal monto,


        LocalDate fechaMovimiento

) {
}
