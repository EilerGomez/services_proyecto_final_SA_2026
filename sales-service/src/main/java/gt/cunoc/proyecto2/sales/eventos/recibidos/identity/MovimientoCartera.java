package gt.cunoc.proyecto2.sales.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Payload de CarteraDebitada y CarteraReembolsada (de Identity, mismo contenido).
 * referenciaTipo (VENTA o ALQUILER) y referenciaId dicen a qué venta u orden corresponde.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovimientoCartera(
        UUID movimientoId,
        String referenciaTipo,
        UUID referenciaId,
        UUID usuarioId,
        BigDecimal monto,
        BigDecimal saldoResultante,
        LocalDate fechaMovimiento) {
}
