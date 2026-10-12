package gt.cunoc.proyecto2.notification.services.recordatorios;

import gt.cunoc.proyecto2.notification.evento.recibido.LicenciaChoferActualizada;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecordatorioLicenciaServiceTest {

    private static final UUID CHOFER = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID SUCURSAL = UUID.randomUUID();
    private static final LocalDate VENCE = LocalDate.of(2027, 3, 15);

    @Mock private JdbcTemplate jdbc;

    @InjectMocks private RecordatorioLicenciaService servicio;

    private static LicenciaChoferActualizada licencia(LocalDate vence) {
        return new LicenciaChoferActualizada(CHOFER, USUARIO, SUCURSAL, vence);
    }

    /** INSERT de un recordatorio: 6 parámetros. */
    private void insertarDevuelve1() {
        lenient().when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    }

    private void verificarProgramado(int dias) {
        verify(jdbc).update(contains("INSERT INTO recordatorios_licencia"), eq(CHOFER), eq(USUARIO), eq(SUCURSAL),
                eq(VENCE), eq(dias), eq(VENCE.minusDays(dias)));
    }

    private void verificarNoProgramado(int dias) {
        verify(jdbc, never()).update(contains("INSERT INTO recordatorios_licencia"), eq(CHOFER), eq(USUARIO),
                eq(SUCURSAL), eq(VENCE), eq(dias), any());
    }

    @Test
    void cancelaLosPendientesYProgramaLosTres() {
        // Arrange
        insertarDevuelve1();

        // Act
        int programados = servicio.programar(licencia(VENCE), LocalDate.of(2026, 10, 1));

        // Assert
        assertEquals(3, programados);
        verify(jdbc).update(contains("estado = 'CANCELADO'"), eq(CHOFER));
        verificarProgramado(30);
        verificarProgramado(15);
        verificarProgramado(3);
    }

    @Test
    void noCreaLosQueYaQuedaronEnElPasado() {
        // Arrange: el evento es del 5-mar; 30 días antes (13-feb) ya pasó, 15 días antes (28-feb) también
        insertarDevuelve1();

        // Act
        int programados = servicio.programar(licencia(VENCE), LocalDate.of(2027, 3, 5));

        // Assert
        assertEquals(1, programados);
        verificarNoProgramado(30);
        verificarNoProgramado(15);
        verificarProgramado(3);
    }

    @Test
    void elMismoDiaProgramadoSiSeCrea() {
        // Arrange: 15 días antes es justo la fecha del evento
        insertarDevuelve1();

        // Act
        int programados = servicio.programar(licencia(VENCE), VENCE.minusDays(15));

        // Assert
        assertEquals(2, programados);
        verificarNoProgramado(30);
        verificarProgramado(15);
        verificarProgramado(3);
    }

    @Test
    void sinFechaDeVencimientoSoloCancela() {
        // Act
        int programados = servicio.programar(licencia(null), LocalDate.of(2026, 10, 1));

        // Assert
        assertEquals(0, programados);
        verify(jdbc).update(contains("estado = 'CANCELADO'"), eq(CHOFER));
        verify(jdbc, never()).update(contains("INSERT"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sinFechaDelEventoUsaHoy() {
        // Arrange: licencia que vence en un año, los tres avisos quedan en el futuro
        insertarDevuelve1();
        LocalDate vence = LocalDate.now().plusYears(1);

        // Act
        int programados = servicio.programar(licencia(vence), null);

        // Assert
        assertEquals(3, programados);
    }
}