package gt.cunoc.proyecto2.sales.dtos.tarifas;

import gt.cunoc.proyecto2.sales.modelos.tarifas.TarifaReembolsoEntidad;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * La tarifa que aplica a una cancelación concreta, ya resuelta. La consulta el frontend para avisarle
 * al cliente antes de pedir el alquiler y para decirle al administrador cuánto se va a devolver antes
 * de confirmar la cancelación; y la usa Sales para calcular el reembolso de verdad.
 *
 * <p>tarifaId es null cuando ninguna fila de la tabla aplica: en ese caso se permite cancelar y se
 * devuelve el 100% (quedarse con el dinero del cliente sin una regla que lo justifique sería peor).</p>
 */
public record ReglaReembolsoRespuesta(
        UUID tarifaId,
        boolean permiteCancelacion,
        BigDecimal porcentajeReembolso,
        String justificacion,
        Integer horasMinimasAntes) {

    private static final String SIN_TARIFA =
            "No hay una tarifa de reembolso configurada para este caso: se devuelve el total pagado";

    public static ReglaReembolsoRespuesta de(TarifaReembolsoEntidad tarifa) {
        return new ReglaReembolsoRespuesta(tarifa.getId(), tarifa.isPermiteCancelacion(),
                tarifa.getPorcentajeReembolso(), tarifa.getJustificacion(), tarifa.getHorasMinimasAntes());
    }

    /** Tabla sin ninguna fila aplicable: se permite cancelar con reembolso total. */
    public static ReglaReembolsoRespuesta porDefecto() {
        return new ReglaReembolsoRespuesta(null, true, TarifaReembolsoEntidad.CIEN.setScale(2),
                SIN_TARIFA, null);
    }

    /** Lo que se devuelve de un monto cobrado, a 2 decimales. */
    public BigDecimal reembolsoDe(BigDecimal montoPagado) {
        return montoPagado.multiply(porcentajeReembolso)
                .divide(TarifaReembolsoEntidad.CIEN, 2, RoundingMode.HALF_UP);
    }
}
