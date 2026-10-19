package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.CodigoDobleFactorSolicitado;
import gt.cunoc.proyecto2.notification.evento.recibido.CuentaCreada;
import gt.cunoc.proyecto2.notification.evento.recibido.RecuperacionContrasenaSolicitada;
import gt.cunoc.proyecto2.notification.evento.recibido.RolAsignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoCuentasService;
import gt.cunoc.proyecto2.notification.services.correo.EnvioCorreoService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManejadoresTest {

    @Mock private EnvioCorreoService correos;

    @Mock private CorreoCuentasService cuentas;

    @InjectMocks
    private ManejadorRecuperacionContrasena manejadorRecuperacion;

    @InjectMocks
    private ManejadorCuentaCreada manejadorCuenta;

    @InjectMocks
    private ManejadorCodigoDobleFactor manejadorCodigo;

    @InjectMocks
    private ManejadorRolAsignado manejadorRol;

    @Test
    void tiposDeEvento() {
        // Act + Assert
        assertEquals("RecuperacionContrasenaSolicitada", manejadorRecuperacion.tipoEvento());
        assertEquals("CuentaCreada", manejadorCuenta.tipoEvento());
        assertEquals("CodigoDobleFactorSolicitado", manejadorCodigo.tipoEvento());
        assertEquals("RolAsignado", manejadorRol.tipoEvento());
        assertEquals(RolAsignado.class, manejadorRol.tipoPayload());
        assertEquals(CuentaCreada.class, manejadorCuenta.tipoPayload());
    }

    @Test
    void recuperacionDelegaEnCorreos() {
        // Arrange
        var evento = new EventoRecibido<>(UUID.randomUUID(), "RecuperacionContrasenaSolicitada",
                1, UUID.randomUUID(), null, Instant.now(), LocalDate.now(), UUID.randomUUID(),
                new RecuperacionContrasenaSolicitada(
                        UUID.randomUUID(), "ana@correo.com", "Ana", "http://x", Instant.now()));

        // Act
        manejadorRecuperacion.manejar(evento);

        // Assert
        verify(correos).enviarRecuperacion(any(), eq("ana@correo.com"), eq("Ana"), any(), any());
    }

    @Test
    void cuentaCreadaDelegaEnCorreos() {
        // Arrange
        CuentaCreada cuenta = new CuentaCreada(UUID.randomUUID(), "ana@correo.com", "Ana", List.of("CLIENTE"),
                null, null);
        var evento = new EventoRecibido<>(UUID.randomUUID(), "CuentaCreada",
                1, UUID.randomUUID(), null, Instant.now(), LocalDate.now(), UUID.randomUUID(), cuenta);

        // Act
        manejadorCuenta.manejar(evento);

        // Assert
        verify(cuentas).bienvenida(evento.eventId(), cuenta);
    }

    @Test
    void rolAsignadoDelegaEnCorreosDeCuentas() {
        // Arrange
        RolAsignado rol = new RolAsignado(UUID.randomUUID(), "ana@correo.com", "Ana", "ADMIN_SISTEMA", null, null);
        var evento = new EventoRecibido<>(UUID.randomUUID(), "RolAsignado",
                1, UUID.randomUUID(), null, Instant.now(), LocalDate.now(), UUID.randomUUID(), rol);

        // Act
        manejadorRol.manejar(evento);

        // Assert
        verify(cuentas).rolAsignado(evento.eventId(), rol);
    }

    @Test
    void codigoDobleFactorDelegaEnCorreos() {
        // Arrange
        var evento = new EventoRecibido<>(UUID.randomUUID(), "CodigoDobleFactorSolicitado",
                1, UUID.randomUUID(), null, Instant.now(), LocalDate.now(), UUID.randomUUID(),
                new CodigoDobleFactorSolicitado(
                        UUID.randomUUID(), "ana@correo.com", "Ana", "123456", Instant.now()));

        // Act
        manejadorCodigo.manejar(evento);

        // Assert
        verify(correos).enviarCodigoDobleFactor(any(), eq("ana@correo.com"), eq("Ana"), eq("123456"), any());
    }
}
