package gt.cunoc.proyecto2.identity.dtos.cartera;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Saldo de la cartera (I17).
 */
public record WalletResponse(

        UUID carteraId,
        BigDecimal saldo,
        Instant fechaActualizacion

) {
}
