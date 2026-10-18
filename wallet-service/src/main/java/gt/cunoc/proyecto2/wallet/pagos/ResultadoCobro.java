package gt.cunoc.proyecto2.wallet.pagos;

/**
 * Respuesta de la pasarela.
 *
 * @param aprobado          true si el dinero ya se cobró
 * @param referenciaExterna id del cobro en la pasarela (se guarda en el movimiento)
 * @param motivo            por qué se rechazó (null si se aprobó)
 */
public record ResultadoCobro(boolean aprobado, String referenciaExterna, String motivo) {

    public static ResultadoCobro aprobado(String referenciaExterna) {
        return new ResultadoCobro(true, referenciaExterna, null);
    }

    public static ResultadoCobro rechazado(String motivo) {
        return new ResultadoCobro(false, null, motivo);
    }
}
