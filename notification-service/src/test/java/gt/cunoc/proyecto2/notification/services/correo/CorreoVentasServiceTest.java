package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerPagado;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoVendido;
import gt.cunoc.proyecto2.notification.evento.recibido.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.notification.evento.recibido.VentaCancelada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorreoVentasServiceTest {

    private static final UUID EVENTO_ID = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 1);
    /** 5-nov 08:00 en Guatemala (14:00 UTC). */
    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T14:00:00Z");

    @Mock private EnvioCorreoService envio;

    private CorreoVentasService servicio;

    private final ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<AdjuntoCorreo> adjunto = ArgumentCaptor.forClass(AdjuntoCorreo.class);

    @BeforeEach
    void preparar() {
        servicio = new CorreoVentasService(envio, JsonMapper.builder().build(), new BoletoPdfService());
    }

    private void capturar(String correo, String plantilla) {
        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq(correo), eq(plantilla), asunto.capture(), datos.capture(),
                cuerpo.capture());
    }

    private void capturarCompra(String correo) {
        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq(correo), eq(CorreoVentasService.COMPRA_BOLETO),
                asunto.capture(), datos.capture(), cuerpo.capture(), adjunto.capture());
    }

    @Test
    void compraBoletoArmaComprobanteConElPdfAdjunto() {
        // Arrange
        UUID ventaId = UUID.randomUUID();
        BoletoVendido compra = new BoletoVendido(ventaId, "1234567-8", "Ana López", "ana@correo.com", List.of(3, 4),
                new BigDecimal("150"), "CARTERA", FECHA, "Quetzaltenango", "Guatemala", SALIDA, "P-123ABC");

        // Act
        servicio.compraBoleto(EVENTO_ID, compra);

        // Assert
        capturarCompra("ana@correo.com");
        assertAll(
                () -> assertEquals("Tu compra Quetzaltenango → Guatemala", asunto.getValue()),
                () -> assertTrue(cuerpo.getValue().startsWith("Hola Ana López,")),
                () -> assertTrue(cuerpo.getValue().contains("Salida: 05/11/2026 08:00")),
                () -> assertTrue(cuerpo.getValue().contains("Asientos: 3, 4")),
                () -> assertTrue(cuerpo.getValue().contains("Total: Q 150.00, pagado con cartera digital")),
                () -> assertTrue(cuerpo.getValue().contains("Fecha de pago: 01/11/2026")),
                () -> assertTrue(cuerpo.getValue().contains(ventaId.toString())),
                () -> assertTrue(datos.getValue().contains("\"asientos\":[3,4]")),
                () -> assertTrue(datos.getValue().contains("\"metodoPago\":\"CARTERA\"")));
        AdjuntoCorreo pdf = adjunto.getValue();
        assertAll(
                () -> assertEquals("boleto-" + ventaId.toString().substring(0, 8) + ".pdf", pdf.nombre()),
                () -> assertEquals("application/pdf", pdf.tipoContenido()),
                () -> assertTrue(pdf.contenido().length > 500),
                () -> assertEquals("%PDF", new String(pdf.contenido(), 0, 4)));
    }

    @Test
    void compraEnMostradorSinCorreoIgualDelegaSinAdjunto() {
        // Arrange: EnvioCorreoService decide no registrar nada cuando no hay correo
        BoletoVendido compra = new BoletoVendido(UUID.randomUUID(), "CF", "Consumidor final", null, List.of(1),
                new BigDecimal("75.00"), "EFECTIVO", FECHA, "Quetzaltenango", "Guatemala", SALIDA, null);

        // Act
        servicio.compraBoleto(EVENTO_ID, compra);

        // Assert
        capturarCompra(null);
        assertAll(
                () -> assertTrue(cuerpo.getValue().contains("Bus: -")),
                () -> assertTrue(cuerpo.getValue().contains("pagado con efectivo")),
                () -> assertNull(adjunto.getValue()));
    }

    @Test
    void ventaCanceladaConCarteraAvisaElReembolso() {
        // Arrange
        VentaCancelada venta = new VentaCancelada(UUID.randomUUID(), "Ana", "ana@correo.com", List.of(1, 2),
                new BigDecimal("150.00"), "CARTERA", "Viaje suspendido", FECHA, "Quetzaltenango", "Guatemala",
                SALIDA);

        // Act
        servicio.ventaCancelada(EVENTO_ID, venta);

        // Assert
        capturar("ana@correo.com", CorreoVentasService.VENTA_CANCELADA);
        assertAll(
                () -> assertEquals("Tu compra fue cancelada", asunto.getValue()),
                () -> assertTrue(cuerpo.getValue().contains("Viaje: Quetzaltenango → Guatemala")),
                () -> assertTrue(cuerpo.getValue().contains("Monto anulado: Q 150.00")),
                () -> assertTrue(cuerpo.getValue().contains("Motivo: Viaje suspendido")),
                () -> assertTrue(cuerpo.getValue().contains("tu cartera digital")),
                () -> assertTrue(datos.getValue().contains("\"reembolsoACartera\":true")));
    }

    @Test
    void ventaCanceladaEnEfectivoSinViaje() {
        // Arrange: el viaje ya se eliminó, no trae origen ni destino
        VentaCancelada venta = new VentaCancelada(UUID.randomUUID(), "Ana", "ana@correo.com", List.of(5),
                new BigDecimal("75.00"), "EFECTIVO", null, FECHA, null, null, null);

        // Act
        servicio.ventaCancelada(EVENTO_ID, venta);

        // Assert
        capturar("ana@correo.com", CorreoVentasService.VENTA_CANCELADA);
        assertAll(
                () -> assertFalse(cuerpo.getValue().contains("Viaje:")),
                () -> assertTrue(cuerpo.getValue().contains("Motivo: -")),
                () -> assertTrue(cuerpo.getValue().contains("Acércate a la sucursal")),
                () -> assertTrue(datos.getValue().contains("\"reembolsoACartera\":false")));
    }

    @Test
    void alquilerPagadoConfirmaElPago() {
        // Arrange: el pago solo le genera correo al cliente; la sucursal se avisa en el panel
        AlquilerPagado alquiler = new AlquilerPagado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Colegio San José", "colegio@correo.com", "Quetzaltenango", "Antigua Guatemala",
                new BigDecimal("2600.00"), "EFECTIVO", FECHA);

        // Act
        servicio.alquilerPagado(EVENTO_ID, alquiler);

        // Assert
        capturar("colegio@correo.com", CorreoVentasService.ALQUILER_PAGADO);
        assertAll(
                () -> assertEquals("Pago de alquiler recibido", asunto.getValue()),
                () -> assertTrue(cuerpo.getValue().contains("Recorrido: Quetzaltenango → Antigua Guatemala")),
                () -> assertTrue(cuerpo.getValue().contains("Monto: Q 2600.00, pagado con efectivo")),
                () -> assertTrue(datos.getValue().contains("\"fechaPago\":\"2026-11-01\"")));
    }

    @Test
    void alquilerAsignadoAvisaAlClienteElBusYElPiloto() {
        // Arrange
        AlquilerAsignado asignado = new AlquilerAsignado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Colegio San José", "colegio@correo.com", "Quetzaltenango", "Antigua Guatemala",
                SALIDA, SALIDA.plusHours(10), "P-123ABC", "Juan Pérez");

        // Act
        servicio.alquilerAsignado(EVENTO_ID, asignado);

        // Assert
        capturar("colegio@correo.com", CorreoVentasService.ALQUILER_ASIGNADO);
        assertAll(
                () -> assertEquals("Tu alquiler ya tiene bus y piloto", asunto.getValue()),
                () -> assertTrue(cuerpo.getValue().contains("Recorrido: Quetzaltenango → Antigua Guatemala")),
                () -> assertTrue(cuerpo.getValue().contains("Salida: 05/11/2026 08:00")),
                () -> assertTrue(cuerpo.getValue().contains("Bus: P-123ABC")),
                () -> assertTrue(cuerpo.getValue().contains("Piloto: Juan Pérez")),
                () -> assertTrue(datos.getValue().contains("\"busPlaca\":\"P-123ABC\"")));
    }

    @Test
    void alquilerAsignadoSinPlacaNiPilotoMuestraGuion() {
        // Arrange
        AlquilerAsignado asignado = new AlquilerAsignado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Colegio", null, "Quetzaltenango", "Antigua", SALIDA, null, null, null);

        // Act
        servicio.alquilerAsignado(EVENTO_ID, asignado);

        // Assert
        capturar(null, CorreoVentasService.ALQUILER_ASIGNADO);
        assertAll(
                () -> assertTrue(cuerpo.getValue().contains("Bus: -")),
                () -> assertTrue(cuerpo.getValue().contains("Piloto: -")),
                () -> assertTrue(cuerpo.getValue().contains("Retorno: -")));
    }

    @Test
    void cotizacionSinRetornoMuestraGuion() {
        // Arrange
        CotizacionAlquilerConfirmada cotizacion = new CotizacionAlquilerConfirmada(UUID.randomUUID(), "Colegio",
                "colegio@correo.com", "Quetzaltenango", "Antigua", SALIDA, null, 30, new BigDecimal("2600.5"));

        // Act
        servicio.cotizacionAlquiler(EVENTO_ID, cotizacion);

        // Assert
        capturar("colegio@correo.com", CorreoVentasService.COTIZACION_ALQUILER);
        assertAll(
                () -> assertEquals("Tu cotización de alquiler está lista", asunto.getValue()),
                () -> assertTrue(cuerpo.getValue().contains("Retorno: -")),
                () -> assertTrue(cuerpo.getValue().contains("Pasajeros: 30")),
                () -> assertTrue(cuerpo.getValue().contains("Precio: Q 2600.50")),
                () -> assertTrue(datos.getValue().contains("\"fechaRetorno\":null")));
    }

    @Test
    void formatosConValoresVacios() {
        // Act + Assert
        assertAll(
                () -> assertEquals("-", CorreoVentasService.quetzales(null)),
                () -> assertEquals("-", CorreoVentasService.asientos(List.of())),
                () -> assertEquals("-", CorreoVentasService.asientos(null)),
                () -> assertEquals("-", CorreoVentasService.fechaHora(null)),
                () -> assertEquals("-", CorreoVentasService.fecha(null)),
                () -> assertEquals("efectivo", CorreoVentasService.metodo(null)));
    }
}