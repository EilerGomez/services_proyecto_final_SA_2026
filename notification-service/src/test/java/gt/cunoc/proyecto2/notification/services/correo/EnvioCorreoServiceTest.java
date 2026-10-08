package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.models.Notificacion;
import gt.cunoc.proyecto2.notification.repositories.NotificacionRepository;
import java.time.Instant;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioCorreoServiceTest {

    private static final UUID EVENTO_ID = UUID.randomUUID();

    @Mock private NotificacionRepository notificaciones;
    @Mock private JavaMailSender correos;

    private EnvioCorreoService servicio;

    @BeforeEach
    void preparar() {
        // Constructor manual: remitente es String y Mockito no lo inyecta.
        servicio = new EnvioCorreoService(notificaciones, correos, "test@proyecto2.local");
    }

    @Test
    void recuperacionNuevaSeRegistraYSeEnvia() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "RECUPERACION_CONTRASENA"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        servicio.enviarRecuperacion(EVENTO_ID, "ana@correo.com", "Ana",
                "http://localhost:5173/restablecer?token=x", Instant.now().plusSeconds(600));

        // Assert
        verify(notificaciones).saveAndFlush(guardada.capture());
        verify(correos).send(enviado.capture());
        assertAll(
                () -> assertEquals("RECUPERACION_CONTRASENA", guardada.getValue().getPlantilla()),
                () -> assertEquals("ENVIADA", guardada.getValue().getEstado()),
                () -> assertEquals("ana@correo.com", enviado.getValue().getTo()[0]));
    }

    @Test
    void correoYaEnviadoNoSeRepite() {
        // Arrange
        Notificacion previa = new Notificacion();
        previa.setEstado("ENVIADA");
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "RECUPERACION_CONTRASENA"))
                .thenReturn(Optional.of(previa));

        // Act
        servicio.enviarRecuperacion(EVENTO_ID, "ana@correo.com", "Ana", "http://x", Instant.now());

        // Assert
        verify(correos, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void falloSmtpMarcaFallidaYPropagaParaReintento() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "RECUPERACION_CONTRASENA"))
                .thenReturn(Optional.empty());
        doThrow(new org.springframework.mail.MailSendException("sin red"))
                .when(correos).send(any(SimpleMailMessage.class));

        // Act + Assert
        assertThrows(org.springframework.mail.MailSendException.class,
                () -> servicio.enviarRecuperacion(
                        EVENTO_ID, "ana@correo.com", "Ana", "http://x", Instant.now()));
        verify(notificaciones).save(any(Notificacion.class));
    }

    @Test
    void bienvenidaConActivacionYLuegoDuplicada() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "CUENTA_CREADA"))
                .thenReturn(Optional.empty());

        // Act
        servicio.enviarBienvenida(EVENTO_ID, "ana@correo.com", "Ana",
                "http://localhost:5173/restablecer?token=x");

        // Assert
        verify(correos).send(any(SimpleMailMessage.class));
    }

    @Test
    void bienvenidaSinActivacionInvitaALogin() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "CUENTA_CREADA"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        servicio.enviarBienvenida(EVENTO_ID, "ana@correo.com", "Ana", null);

        // Assert
        verify(correos).send(enviado.capture());
        assertTrue(enviado.getValue().getText().contains("iniciar sesión"));
    }

    @Test
    void codigoDobleFactorSeEnvia() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "CODIGO_DOBLE_FACTOR"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        servicio.enviarCodigoDobleFactor(EVENTO_ID, "ana@correo.com", "Ana",
                "123456", Instant.now().plusSeconds(600));

        // Assert
        verify(correos).send(enviado.capture());
        assertTrue(enviado.getValue().getText().contains("123456"));
    }

    @Test
    void registroDuplicadoConcurrenteNoEnvia() {
        // Arrange
        when(notificaciones.findByEventoIdAndDestinatarioAndPlantilla(
                EVENTO_ID, "ana@correo.com", "RECUPERACION_CONTRASENA"))
                .thenReturn(Optional.empty());
        when(notificaciones.saveAndFlush(any(Notificacion.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicada"));

        // Act
        servicio.enviarRecuperacion(EVENTO_ID, "ana@correo.com", "Ana",
                "http://x", Instant.now().plusSeconds(600));

        // Assert
        verify(correos, never()).send(any(SimpleMailMessage.class));
    }
}
