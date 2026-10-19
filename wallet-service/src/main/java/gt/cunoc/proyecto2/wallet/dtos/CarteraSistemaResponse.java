package gt.cunoc.proyecto2.wallet.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Billetera del sistema (W4): saldo y totales históricos de cobros (compras en línea) y devoluciones
 * (reembolsos). saldo = totalCobrado - totalDevuelto.
 */
public record CarteraSistemaResponse(

        UUID carteraId,
        BigDecimal saldo,
        BigDecimal totalCobrado,
        BigDecimal totalDevuelto,
        Instant fechaActualizacion

) {
}
