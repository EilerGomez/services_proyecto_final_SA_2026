package gt.cunoc.proyecto2.wallet.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Solicitud de reembolso de cartera publicada por Sales (compensación).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReembolsoCarteraSolicitado(

        String referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        String motivo,
        LocalDate fechaMovimiento

) {
}
