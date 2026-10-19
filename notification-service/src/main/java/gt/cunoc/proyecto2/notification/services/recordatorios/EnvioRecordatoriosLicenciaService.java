package gt.cunoc.proyecto2.notification.services.recordatorios;

import gt.cunoc.proyecto2.notification.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.notification.clientes.ContactoUsuario;
import gt.cunoc.proyecto2.notification.services.correo.CorreoRecordatoriosService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/*
 * Envía los recordatorios de licencia cuya fecha ya llegó (fecha_programada <= hoy).
 * El correo y el nombre se piden a Identity (R4) con el usuario_id guardado al programar.
 * Cada aviso es idempotente por el id de la fila: repetir la corrida no duplica correos.
 */
@Service
public class EnvioRecordatoriosLicenciaService {

    private static final Logger log = LoggerFactory.getLogger(EnvioRecordatoriosLicenciaService.class);

    private static final String PENDIENTES = """
            SELECT id, usuario_id, fecha_vencimiento, dias_antes
            FROM recordatorios_licencia
            WHERE estado = 'PENDIENTE' AND fecha_programada <= ?
            ORDER BY fecha_programada""";

    private static final String MARCAR_ENVIADO = """
            UPDATE recordatorios_licencia SET estado = 'ENVIADO', fecha_envio = now() WHERE id = ?""";

    private final JdbcTemplate jdbc;
    private final ClienteIdentity identity;
    private final CorreoRecordatoriosService correos;

    public EnvioRecordatoriosLicenciaService(JdbcTemplate jdbc, ClienteIdentity identity,
                                             CorreoRecordatoriosService correos) {
        this.jdbc = jdbc;
        this.identity = identity;
        this.correos = correos;
    }

    /**
     * Envía todos los recordatorios pendientes con fecha_programada <= hoy.
     * Un recordatorio que falle (Identity o el SMTP) queda PENDIENTE para el próximo intento.
     *
     * @return cuántos recordatorios se enviaron
     */
    public int enviarPendientes(LocalDate hoy) {
        List<Recordatorio> pendientes = jdbc.query(PENDIENTES,
                (rs, fila) -> new Recordatorio(
                        rs.getObject("id", UUID.class),
                        rs.getObject("usuario_id", UUID.class),
                        rs.getDate("fecha_vencimiento").toLocalDate(),
                        rs.getInt("dias_antes")),
                hoy);
        int enviados = 0;
        for (Recordatorio recordatorio : pendientes) {
            if (enviar(recordatorio)) {
                enviados++;
            }
        }
        return enviados;
    }

    private boolean enviar(Recordatorio recordatorio) {
        try {
            Optional<ContactoUsuario> contacto = identity.contacto(recordatorio.usuarioId());
            if (contacto.isEmpty() || contacto.get().correo() == null || contacto.get().correo().isBlank()) {
                log.warn("Recordatorio de licencia {} sin contacto en Identity; se reintentará", recordatorio.id());
                return false;
            }
            correos.licenciaPorVencer(recordatorio.id(), contacto.get().correo(), contacto.get().nombre(),
                    recordatorio.fechaVencimiento(), recordatorio.diasAntes());
            jdbc.update(MARCAR_ENVIADO, recordatorio.id());
            return true;
        } catch (RuntimeException falla) {
            log.warn("No se pudo enviar el recordatorio de licencia {}: {}", recordatorio.id(), falla.getMessage());
            return false;
        }
    }

    /* Fila de recordatorios_licencia que ya toca enviar. */
    record Recordatorio(UUID id, UUID usuarioId, LocalDate fechaVencimiento, int diasAntes) {
    }}
