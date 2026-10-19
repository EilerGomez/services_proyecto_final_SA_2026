package gt.cunoc.proyecto2.notification.services.correo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CorreoRecordatoriosServiceTest {

    private static final UUID RECORDATORIO = UUID.randomUUID();

    @Mock private EnvioCorreoService envio;

    private CorreoRecordatoriosService servicio;

    private final ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void preparar() {
        servicio = new CorreoRecordatoriosService(envio, JsonMapper.builder().build());
    }

    private void capturar() {
        verify(envio).enviarPlantilla(eq(RECORDATORIO), eq("juan@correo.com"),
                eq(CorreoRecordatoriosService.LICENCIA_POR_VENCER), asunto.capture(), datos.capture(),
                cuerpo.capture());
    }

    @Test
    void armaElAvisoDeVencimiento() {
        // Act
        servicio.licenciaPorVencer(RECORDATORIO, "juan@correo.com", "Juan Pérez", LocalDate.of(2026, 11, 4), 3);

        // Assert
        capturar();
        assertAll(
                () -> assertEquals("Tu licencia de conducir está por vencer", asunto.getValue()),
                () -> assertTrue(cuerpo.getValue().contains("Hola Juan Pérez")),
                () -> assertTrue(cuerpo.getValue().contains("vence el 04/11/2026 (en 3 días)")),
                () -> assertTrue(datos.getValue().contains("\"diasAntes\":3")),
                () -> assertTrue(datos.getValue().contains("\"fechaVencimiento\":\"2026-11-04\"")));
    }

    @Test
    void sinFechaDeVencimientoMuestraGuion() {
        // Act
        servicio.licenciaPorVencer(RECORDATORIO, "juan@correo.com", "Juan", null, 30);

        // Assert
        capturar();
        assertAll(
                () -> assertTrue(cuerpo.getValue().contains("vence el - (en 30 días)")),
                () -> assertTrue(datos.getValue().contains("\"fechaVencimiento\":null")));
    }
}
