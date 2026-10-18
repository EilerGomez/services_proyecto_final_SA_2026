package gt.cunoc.proyecto2.wallet.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Saldo de la cartera (W1).
 */
public record WalletResponse(

        UUID carteraId,
        BigDecimal saldo,
        Instant fechaActualizacion

) {
}
