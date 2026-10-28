package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de AlquilerCancelado (Trips). Trips ya canceló el alquiler y borró su viaje; acá se decide
 * qué pasa con el dinero.
 *
 * <p>No trae el porcentaje de reembolso a propósito: trae los datos crudos (en qué estado estaba el
 * alquiler y cuántas horas faltaban para la salida) y Sales resuelve la tarifa con su propia tabla,
 * que es la dueña de la política. Así el número que se paga siempre sale de tarifas_reembolso y queda
 * guardado en la orden para poder explicarlo después.</p>
 *
 * <p>estadoAnterior llega como texto (es la máquina de estados de Trips, no de Sales): se traduce con
 * EstadoAlquilerTarifa.deNombre, que cae a CUALQUIERA si aparece un estado nuevo.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlquilerCancelado(
        UUID alquilerId,
        UUID sucursalId,
        String estadoAnterior,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaHoraCancelacion,
        Long horasAntesDeSalida,
        String motivo,
        UUID viajeId,
        String clienteNombre,
        String clienteCorreo) {

    /** Horas que faltaban para la salida, nunca negativas (si el viaje ya salió, cero). */
    public long horas() {
        return horasAntesDeSalida == null ? 0L : Math.max(horasAntesDeSalida, 0L);
    }
}
