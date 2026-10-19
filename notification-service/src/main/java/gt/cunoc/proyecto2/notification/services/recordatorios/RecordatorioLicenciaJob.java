package gt.cunoc.proyecto2.notification.services.recordatorios;

import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * Job diario que envía los recordatorios de licencia cuya fecha ya llegó.
 * Hora y periodicidad se ajustan con app.recordatorios.cron; se apaga con app.recordatorios.habilitado=false.
 */
@Component
@ConditionalOnProperty(prefix = "app.recordatorios", name = "habilitado", havingValue = "true", matchIfMissing = true)
public class RecordatorioLicenciaJob {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioLicenciaJob.class);
    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");

    private final EnvioRecordatoriosLicenciaService envio;

    public RecordatorioLicenciaJob(EnvioRecordatoriosLicenciaService envio) {
        this.envio = envio;
    }

    @Scheduled(cron = "${app.recordatorios.cron:0 0 7 * * *}", zone = "America/Guatemala")
    public void enviarRecordatorios() {
        int enviados = envio.enviarPendientes(LocalDate.now(ZONA_LOCAL));
        log.info("Recordatorios de licencia enviados: {}", enviados);
    }
}
