package gt.cunoc.proyecto2.notification.services.recordatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.notification.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.notification.clientes.ContactoUsuario;
import gt.cunoc.proyecto2.notification.services.correo.CorreoRecordatoriosService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class EnvioRecordatoriosLicenciaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 1);
    private static final UUID RECORDATORIO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final LocalDate VENCE = LocalDate.of(2026, 11, 4);

    @Mock private JdbcTemplate jdbc;
    @Mock private ClienteIdentity identity;
    @Mock private CorreoRecordatoriosService correos;

    @InjectMocks private EnvioRecordatoriosLicenciaService servicio;

    /** Simula el SELECT de recordatorios pendientes con las filas indicadas. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void hayPendientes(EnvioRecordatoriosLicenciaService.Recordatorio... filas) {
        doReturn(List.of(filas)).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    private void contacto(String correo) {
        when(identity.contacto(USUARIO))
                .thenReturn(Optional.of(new ContactoUsuario(USUARIO, "Juan Pérez", correo, null)));
    }

    @Test
    void enviaElPendienteYLoMarcaEnviado() {
        // Arrange
        hayPendientes(new EnvioRecordatoriosLicenciaService.Recordatorio(RECORDATORIO, USUARIO, VENCE, 3));
        contacto("juan@correo.com");

        // Act
        int enviados = servicio.enviarPendientes(HOY);

        // Assert
        assertEquals(1, enviados);
        verify(correos).licenciaPorVencer(RECORDATORIO, "juan@correo.com", "Juan Pérez", VENCE, 3);
        verify(jdbc).update(contains("estado = 'ENVIADO'"), eq(RECORDATORIO));
    }

    @Test
    void sinPendientesNoHaceNada() {
        // Arrange
        hayPendientes();

        // Act + Assert
        assertEquals(0, servicio.enviarPendientes(HOY));
        verify(correos, never()).licenciaPorVencer(any(), any(), any(), any(), anyInt());
    }

    @Test
    void sinContactoEnIdentityNoEnviaNiMarca() {
        // Arrange
        hayPendientes(new EnvioRecordatoriosLicenciaService.Recordatorio(RECORDATORIO, USUARIO, VENCE, 15));
        when(identity.contacto(USUARIO)).thenReturn(Optional.empty());

        // Act + Assert
        assertEquals(0, servicio.enviarPendientes(HOY));
        verify(correos, never()).licenciaPorVencer(any(), any(), any(), any(), anyInt());
        verify(jdbc, never()).update(contains("ENVIADO"), eq(RECORDATORIO));
    }

    @Test
    void contactoConCorreoEnBlancoNoEnvia() {
        // Arrange
        hayPendientes(new EnvioRecordatoriosLicenciaService.Recordatorio(RECORDATORIO, USUARIO, VENCE, 30));
        contacto("   ");

        // Act + Assert
        assertEquals(0, servicio.enviarPendientes(HOY));
        verify(jdbc, never()).update(contains("ENVIADO"), eq(RECORDATORIO));
    }

    @Test
    void siElEnvioFallaElRecordatorioQuedaPendiente() {
        // Arrange
        hayPendientes(new EnvioRecordatoriosLicenciaService.Recordatorio(RECORDATORIO, USUARIO, VENCE, 3));
        contacto("juan@correo.com");
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP caído"))
                .when(correos).licenciaPorVencer(RECORDATORIO, "juan@correo.com", "Juan Pérez", VENCE, 3);

        // Act + Assert
        assertEquals(0, servicio.enviarPendientes(HOY));
        verify(jdbc, never()).update(contains("ENVIADO"), eq(RECORDATORIO));
    }

    @Test
    void siIdentityFallaElRecordatorioQuedaPendiente() {
        // Arrange
        hayPendientes(new EnvioRecordatoriosLicenciaService.Recordatorio(RECORDATORIO, USUARIO, VENCE, 3));
        when(identity.contacto(USUARIO)).thenThrow(new IllegalStateException("identity caído"));

        // Act + Assert
        assertEquals(0, servicio.enviarPendientes(HOY));
        verify(jdbc, never()).update(contains("ENVIADO"), eq(RECORDATORIO));
    }
}
