package gt.cunoc.proyecto2.notification.services.recordatorios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordatorioLicenciaJobTest {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");

    @Mock private EnvioRecordatoriosLicenciaService envio;

    @InjectMocks private RecordatorioLicenciaJob job;

    @Test
    void enviaLosRecordatoriosDelDiaEnGuatemala() {
        // Arrange
        when(envio.enviarPendientes(any(LocalDate.class))).thenReturn(2);

        // Act
        job.enviarRecordatorios();

        // Assert
        verify(envio).enviarPendientes(LocalDate.now(ZONA_LOCAL));
    }
}
