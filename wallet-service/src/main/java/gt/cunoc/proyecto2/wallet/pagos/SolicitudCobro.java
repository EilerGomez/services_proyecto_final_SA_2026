package gt.cunoc.proyecto2.wallet.pagos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos que se mandan a la pasarela. No incluye datos de tarjeta: esos los captura la pasarela en su
 * propio formulario, así nunca pasan por este sistema.
 *
 * @param idempotencia id único del intento; la pasarela no cobra dos veces el mismo
 */
public record SolicitudCobro(UUID idempotencia, UUID usuarioId, BigDecimal monto, String moneda) {

    public static final String QUETZALES = "GTQ";
}
