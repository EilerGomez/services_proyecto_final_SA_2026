package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaReembolsoEntidad;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Qué pasaría si este alquiler se cancelara en este momento. La consulta el frontend para avisarle al
 * cliente (al pedir el alquiler y antes de confirmar la cancelación) y para deshabilitar el botón
 * cuando la cancelación ya no se permite.
 *
 * <p>El porcentaje es informativo: el monto que se paga de verdad lo calcula Sales con su propia
 * tabla, que es la dueña de la política. Acá sale de la copia local (vista_tarifas_reembolso).</p>
 */
public record PoliticaCancelacionRespuesta(
        UUID tarifaId,
        boolean permiteCancelacion,
        BigDecimal porcentajeReembolso,
        String justificacion,
        /** Horas que faltan para la salida; negativa si el viaje ya debió salir. */
        long horasAntesDeSalida,
        /** Lo que se le devolvería del precio confirmado, o null si el alquiler todavía no se pagó. */
        BigDecimal montoEstimadoReembolso) {

    private static final String SIN_TARIFA =
            "No hay una tarifa de reembolso configurada para este caso: se devolvería el total pagado";

    public static PoliticaCancelacionRespuesta de(VistaTarifaReembolsoEntidad tarifa, long horas,
                                                  BigDecimal montoEstimado) {
        return new PoliticaCancelacionRespuesta(tarifa.getId(), tarifa.isPermiteCancelacion(),
                tarifa.getPorcentajeReembolso(), tarifa.getJustificacion(), horas, montoEstimado);
    }

    /** Ninguna fila de la copia aplica: se permite cancelar con devolución total. */
    public static PoliticaCancelacionRespuesta porDefecto(long horas, BigDecimal montoEstimado) {
        return new PoliticaCancelacionRespuesta(null, true, new BigDecimal("100.00"), SIN_TARIFA,
                horas, montoEstimado);
    }
}
