package gt.cunoc.proyecto2.sales.dtos.tarifas;

import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.tarifas.TarifaReembolsoEntidad;
import java.math.BigDecimal;
import java.util.UUID;

/** Una fila de la tabla de tarifas de reembolso (pantalla de administración). */
public record TarifaReembolsoRespuesta(
        UUID tarifaId,
        EstadoAlquilerTarifa estadoAlquiler,
        int horasMinimasAntes,
        boolean permiteCancelacion,
        BigDecimal porcentajeReembolso,
        String justificacion,
        boolean activa,
        Integer version) {

    public TarifaReembolsoRespuesta(TarifaReembolsoEntidad tarifa) {
        this(tarifa.getId(),
                tarifa.getEstadoAlquiler(),
                tarifa.getHorasMinimasAntes(),
                tarifa.isPermiteCancelacion(),
                tarifa.getPorcentajeReembolso(),
                tarifa.getJustificacion(),
                tarifa.isActiva(),
                tarifa.getVersion());
    }
}
