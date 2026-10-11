package gt.cunoc.proyecto2.sales.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/*
 * Payload de DebitoCarteraRechazado (de Identity).
 * motivo: SALDO_INSUFICIENTE, USUARIO_INACTIVO o CARTERA_INEXISTENTE.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebitoCarteraRechazado(
        String referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        String motivo) {
}
