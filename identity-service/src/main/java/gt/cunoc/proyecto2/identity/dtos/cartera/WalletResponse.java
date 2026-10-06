package gt.cunoc.proyecto2.identity.dtos.cartera;


import java.math.BigDecimal;
import java.util.UUID;


/**
 * Vista del saldo de una cartera.
 */
public record WalletResponse(
    UUID usuarioId,
    BigDecimal saldo
) {}
