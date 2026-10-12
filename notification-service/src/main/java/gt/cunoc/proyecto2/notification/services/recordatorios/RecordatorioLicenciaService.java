package gt.cunoc.proyecto2.notification.services.recordatorios;

import gt.cunoc.proyecto2.notification.evento.recibido.LicenciaChoferActualizada;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/*
 * Programa los recordatorios de vencimiento de licencia (tabla recordatorios_licencia, V1).
 * Aquí no se envía nada: la tarea de recordatorios toma los PENDIENTE cuya fecha ya llegó.
 */
@Service
public class RecordatorioLicenciaService {

    /** Días antes del vencimiento en que se avisa (CHECK de la tabla). */
    static final int[] DIAS_ANTES = {30, 15, 3};

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");

    private static final String CANCELAR_PENDIENTES = """
            UPDATE recordatorios_licencia SET estado = 'CANCELADO'
            WHERE chofer_id = ? AND estado = 'PENDIENTE'""";

    // Si la licencia vuelve a una fecha que ya tuvo, el recordatorio cancelado se reactiva
    private static final String PROGRAMAR = """
            INSERT INTO recordatorios_licencia (chofer_id, usuario_id, sucursal_id, fecha_vencimiento,
                                                dias_antes, fecha_programada)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (chofer_id, fecha_vencimiento, dias_antes)
            DO UPDATE SET estado = 'PENDIENTE', fecha_envio = NULL, usuario_id = EXCLUDED.usuario_id,
                          sucursal_id = EXCLUDED.sucursal_id""";

    private final JdbcTemplate jdbc;

    public RecordatorioLicenciaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /*
     * Cancela los recordatorios pendientes de la licencia anterior y programa los de la nueva.
     * Los que ya quedaron antes de la fecha del evento no se crean (no se mandan 3 avisos de golpe).
     *
     * @param fechaEvento businessDate del evento (fecha de registro en Fleet); si falta, hoy en Guatemala
     * @return cuántos recordatorios quedaron programados
     */
    @Transactional
    public int programar(LicenciaChoferActualizada licencia, LocalDate fechaEvento) {
        jdbc.update(CANCELAR_PENDIENTES, licencia.choferId());
        LocalDate vence = licencia.fechaVencimientoLicencia();
        if (vence == null) {
            return 0;
        }
        LocalDate desde = fechaEvento != null ? fechaEvento : LocalDate.now(ZONA_LOCAL);
        int programados = 0;
        for (int dias : DIAS_ANTES) {
            LocalDate programada = vence.minusDays(dias);
            if (programada.isBefore(desde)) {
                continue;
            }
            programados += jdbc.update(PROGRAMAR, licencia.choferId(), licencia.usuarioId(), licencia.sucursalId(),
                    vence, dias, programada);
        }
        return programados;
    }
}