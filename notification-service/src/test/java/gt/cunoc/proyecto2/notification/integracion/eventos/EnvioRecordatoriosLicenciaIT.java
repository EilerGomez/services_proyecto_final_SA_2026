package gt.cunoc.proyecto2.notification.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.notification.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.notification.services.recordatorios.EnvioRecordatoriosLicenciaService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Envío real de los recordatorios de licencia: Journey Fleet → programar → Identity → correo. */
class EnvioRecordatoriosLicenciaIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired private EnvioRecordatoriosLicenciaService envio;

    @Test
    void enviaElRecordatorioVencidoYLoMarcaEnviado() {
        // Arrange: si vence en 3 días, el aviso de 3 días antes queda programado justo para hoy
        LocalDate hoy = LocalDate.now(ZONA_LOCAL);
        UUID chofer = UUID.randomUUID();
        UUID eventId = publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", hoy.toString(),
                """
                {"choferId":"%s","usuarioId":"%s","sucursalId":"%s","fechaVencimientoLicencia":"%s"}
                """.formatted(chofer, UUID.randomUUID(), UUID.randomUUID(), hoy.plusDays(3)));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(totalCorreos()).isZero();   // programar no envía nada

        // Act
        int enviados = envio.enviarPendientes(hoy);

        // Assert: se pide el contacto a Identity, se manda el correo y la fila queda ENVIADO
        assertThat(enviados).isEqualTo(1);
        assertThat(asuntosPara(CHOFER_CORREO)).containsExactly("Tu licencia de conducir está por vencer");
        assertThat(textoPara(CHOFER_CORREO)).contains("Hola Chofer Prueba",
                "vence el " + FECHA.format(hoy.plusDays(3)) + " (en 3 días)");
        assertThat(jdbc.queryForObject("SELECT estado FROM recordatorios_licencia WHERE chofer_id = ?",
                String.class, chofer)).isEqualTo("ENVIADO");
        assertThat(jdbc.queryForObject("SELECT plantilla FROM notificaciones", String.class))
                .isEqualTo("LICENCIA_POR_VENCER");

        // Repetir la corrida no vuelve a mandar el mismo aviso
        assertThat(envio.enviarPendientes(hoy)).isZero();
        assertThat(totalCorreos()).isEqualTo(1);
    }
}
