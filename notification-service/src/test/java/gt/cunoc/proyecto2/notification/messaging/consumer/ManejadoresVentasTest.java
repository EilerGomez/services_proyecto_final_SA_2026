package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerPagado;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoVendido;
import gt.cunoc.proyecto2.notification.evento.recibido.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.notification.evento.recibido.LicenciaChoferActualizada;
import gt.cunoc.proyecto2.notification.evento.recibido.VentaCancelada;
import gt.cunoc.proyecto2.notification.services.correo.CorreoVentasService;
import gt.cunoc.proyecto2.notification.services.recordatorios.RecordatorioLicenciaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManejadoresVentasTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 1);
    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T08:00:00-06:00");

    @Mock private CorreoVentasService correos;
    @Mock private RecordatorioLicenciaService recordatorios;

    @InjectMocks private ManejadorBoletoVendido manejadorBoleto;
    @InjectMocks private ManejadorVentaCancelada manejadorCancelada;
    @InjectMocks private ManejadorAlquilerPagado manejadorAlquiler;
    @InjectMocks private ManejadorAlquilerAsignado manejadorAsignado;
    @InjectMocks private ManejadorCotizacionAlquilerConfirmada manejadorCotizacion;
    @InjectMocks private ManejadorLicenciaChoferActualizada manejadorLicencia;

    private static <T> EventoRecibido<T> evento(String tipo, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, UUID.randomUUID(), 1L, Instant.now(), FECHA,
                UUID.randomUUID(), payload);
    }

    @Test
    void tiposDeEventoYPayload() {
        // Act + Assert
        assertEquals("BoletoVendido", manejadorBoleto.tipoEvento());
        assertEquals("VentaCancelada", manejadorCancelada.tipoEvento());
        assertEquals("AlquilerPagado", manejadorAlquiler.tipoEvento());
        assertEquals("AlquilerAsignado", manejadorAsignado.tipoEvento());
        assertEquals("CotizacionAlquilerConfirmada", manejadorCotizacion.tipoEvento());
        assertEquals("LicenciaChoferActualizada", manejadorLicencia.tipoEvento());
        assertEquals(BoletoVendido.class, manejadorBoleto.tipoPayload());
        assertEquals(VentaCancelada.class, manejadorCancelada.tipoPayload());
        assertEquals(AlquilerPagado.class, manejadorAlquiler.tipoPayload());
        assertEquals(AlquilerAsignado.class, manejadorAsignado.tipoPayload());
        assertEquals(CotizacionAlquilerConfirmada.class, manejadorCotizacion.tipoPayload());
        assertEquals(LicenciaChoferActualizada.class, manejadorLicencia.tipoPayload());
    }

    @Test
    void boletoVendidoDelegaConElEventId() {
        // Arrange
        BoletoVendido compra = new BoletoVendido(UUID.randomUUID(), "Ana", "ana@correo.com", List.of(1, 2),
                new BigDecimal("150.00"), "CARTERA", FECHA, "Quetzaltenango", "Guatemala", SALIDA, "P-123ABC");
        EventoRecibido<BoletoVendido> evento = evento("BoletoVendido", compra);

        // Act
        manejadorBoleto.manejar(evento);

        // Assert
        verify(correos).compraBoleto(evento.eventId(), compra);
    }

    @Test
    void ventaCanceladaDelegaConElEventId() {
        // Arrange
        VentaCancelada venta = new VentaCancelada(UUID.randomUUID(), "Ana", "ana@correo.com", List.of(1),
                new BigDecimal("75.00"), "EFECTIVO", "Viaje suspendido", FECHA, null, null, null);
        EventoRecibido<VentaCancelada> evento = evento("VentaCancelada", venta);

        // Act
        manejadorCancelada.manejar(evento);

        // Assert
        verify(correos).ventaCancelada(evento.eventId(), venta);
    }

    @Test
    void alquilerPagadoDelegaConElEventId() {
        // Arrange
        AlquilerPagado alquiler = new AlquilerPagado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Colegio", "colegio@correo.com", "Quetzaltenango", "Antigua", new BigDecimal("2600.00"),
                "EFECTIVO", FECHA);
        EventoRecibido<AlquilerPagado> evento = evento("AlquilerPagado", alquiler);

        // Act
        manejadorAlquiler.manejar(evento);

        // Assert
        verify(correos).alquilerPagado(evento.eventId(), alquiler);
    }

    @Test
    void alquilerAsignadoDelegaConElEventId() {
        // Arrange
        AlquilerAsignado asignado = new AlquilerAsignado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Colegio", "colegio@correo.com", "Quetzaltenango", "Antigua", SALIDA, SALIDA.plusHours(10),
                "P-123ABC", "Juan Pérez");
        EventoRecibido<AlquilerAsignado> evento = evento("AlquilerAsignado", asignado);

        // Act
        manejadorAsignado.manejar(evento);

        // Assert
        verify(correos).alquilerAsignado(evento.eventId(), asignado);
    }

    @Test
    void cotizacionDelegaConElEventId() {
        // Arrange
        CotizacionAlquilerConfirmada cotizacion = new CotizacionAlquilerConfirmada(UUID.randomUUID(), "Colegio",
                "colegio@correo.com", "Quetzaltenango", "Antigua", SALIDA, SALIDA.plusHours(10), 30,
                new BigDecimal("2600.00"));
        EventoRecibido<CotizacionAlquilerConfirmada> evento = evento("CotizacionAlquilerConfirmada", cotizacion);

        // Act
        manejadorCotizacion.manejar(evento);

        // Assert
        verify(correos).cotizacionAlquiler(evento.eventId(), cotizacion);
    }

    @Test
    void licenciaProgramaRecordatoriosConLaFechaDelEvento() {
        // Arrange
        LicenciaChoferActualizada licencia = new LicenciaChoferActualizada(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDate.of(2027, 3, 15));
        EventoRecibido<LicenciaChoferActualizada> evento = evento("LicenciaChoferActualizada", licencia);

        // Act
        manejadorLicencia.manejar(evento);

        // Assert
        verify(recordatorios).programar(licencia, FECHA);
    }
}