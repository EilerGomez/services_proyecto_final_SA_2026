package gt.cunoc.proyecto2.sales.eventos.publicados.tarifas;

import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.tarifas.TarifaReembolsoEntidad;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de TarifaReembolsoCambiada. Consume: Trips, que lleva una copia de solo lectura
 * (vista_tarifas_reembolso) para poder rechazar una cancelación sin preguntarle nada a Sales.
 * El porcentaje que se paga de verdad siempre lo calcula Sales con su propia tabla.
 */
public record TarifaReembolsoCambiada(
        UUID tarifaId,
        EstadoAlquilerTarifa estadoAlquiler,
        int horasMinimasAntes,
        boolean permiteCancelacion,
        BigDecimal porcentajeReembolso,
        String justificacion,
        boolean activa) {

    public static TarifaReembolsoCambiada desde(TarifaReembolsoEntidad tarifa) {
        return new TarifaReembolsoCambiada(tarifa.getId(), tarifa.getEstadoAlquiler(),
                tarifa.getHorasMinimasAntes(), tarifa.isPermiteCancelacion(),
                tarifa.getPorcentajeReembolso(), tarifa.getJustificacion(), tarifa.isActiva());
    }
}
