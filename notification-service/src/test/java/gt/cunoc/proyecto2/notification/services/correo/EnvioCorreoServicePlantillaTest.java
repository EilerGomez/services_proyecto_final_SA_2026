package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.models.Notificacion;
import gt.cunoc.proyecto2.notification.repositories.NotificacionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** enviarPlantilla: el método genérico que usan los correos de ventas y alquileres. */
@ExtendWith(MockitoExtension.class)
class EnvioCorreoServicePlantillaTest {

    private static final UUID EVENTO_ID = UUID.randomUUID();

    @Mock private NotificacionRepository notificaciones;
    @Mock private JavaMailSender correos;

    private EnvioCorreoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new EnvioCorreoService(notificaciones, correos, "test@proyecto2.local");
    }

    @Test
    void sinCorreoNoRegistraNiEnvia() {
        // Act
        servicio.enviarPlantilla(EVENTO_ID, null, "COMPRA_BOLETO", "Asunto", "{}", "Cuerpo");
        servicio.enviarPlantilla(EVENTO_ID, "  ", "COMPRA_BOLETO", "Asunto", "{}", "Cuerpo");

        // Assert
        verifyNoInteractions(notificaciones, correos);
    }

    @Test
    void nuevaSeRegistraYSeEnvia() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(EVENTO_ID, "ana@correo.com", "COMPRA_BOLETO"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        servicio.enviarPlantilla(EVENTO_ID, "ana@correo.com", "COMPRA_BOLETO", "Tu compra",
                "{\"total\":150.00}", "Hola Ana");

        // Assert
        verify(notificaciones).saveAndFlush(guardada.capture());
        verify(correos).send(enviado.capture());
        assertAll(
                () -> assertEquals("COMPRA_BOLETO", guardada.getValue().getPlantilla()),
                () -> assertEquals("{\"total\":150.00}", guardada.getValue().getDatos()),
                () -> assertEquals("ENVIADA", guardada.getValue().getEstado()),
                () -> assertEquals("Tu compra", enviado.getValue().getSubject()),
                () -> assertEquals("Hola Ana", enviado.getValue().getText()),
                () -> assertEquals("ana@correo.com", enviado.getValue().getTo()[0]));
    }

    @Test
    void yaEnviadaNoSeRepite() {
        // Arrange
        Notificacion previa = new Notificacion();
        previa.setEstado("ENVIADA");
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(EVENTO_ID, "ana@correo.com", "VENTA_CANCELADA"))
                .thenReturn(Optional.of(previa));

        // Act
        servicio.enviarPlantilla(EVENTO_ID, "ana@correo.com", "VENTA_CANCELADA", "Asunto", "{}", "Cuerpo");

        // Assert
        verify(correos, never()).send(any(SimpleMailMessage.class));
    }
}