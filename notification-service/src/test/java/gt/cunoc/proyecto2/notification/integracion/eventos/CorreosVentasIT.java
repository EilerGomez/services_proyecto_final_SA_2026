package gt.cunoc.proyecto2.notification.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.notification.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorreosVentasIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String ANA = "ana@correo.com";

    private static String boletoVendido(String correo) {
        return """
                {"ventaId":"%s","viajeId":"%s","rutaId":"%s","busId":"%s","busPlaca":"P-123ABC",
                 "sucursalId":"%s","canal":"EN_LINEA","clienteUsuarioId":"%s","clienteNit":"1234567-8",
                 "clienteNombre":"Ana López","clienteCorreo":%s,"asientos":[3,4],"cantidadBoletos":2,
                 "montoTotal":150.00,"metodoPago":"CARTERA","fechaPago":"2026-11-01",
                 "origenNombre":"Quetzaltenango","destinoNombre":"Guatemala",
                 "fechaSalida":"2026-11-05T08:00:00-06:00"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), correo == null ? "null" : "\"" + correo + "\"");
    }

    @Test
    void boletoVendidoEnviaElComprobante() {
        UUID eventId = publicarEvento(SALES_EVENTOS, "BoletoVendido", "2026-11-01", boletoVendido(ANA));

        await().atMost(ESPERA).until(() -> procesado(eventId));

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara(ANA)).containsExactly("Tu compra Quetzaltenango → Guatemala"));
        String texto = textoPara(ANA);
        assertThat(texto).contains("Hola Ana López", "Asientos: 3, 4", "Total: Q 150.00, pagado con cartera digital",
                "Salida: 05/11/2026 08:00", "Bus: P-123ABC");
        assertThat(detallePara(ANA)).contains("\"FileName\":\"boleto-", "application/pdf");

        List<Map<String, Object>> filas = notificaciones(eventId);
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0)).containsEntry("plantilla", "COMPRA_BOLETO")
                .containsEntry("destinatario", ANA)
                .containsEntry("estado", "ENVIADA")
                .containsEntry("intentos", 1);
        assertThat((String) filas.get(0).get("datos")).contains("asientos", "CARTERA");
        assertThat(filas.get(0).get("fecha_envio")).isNotNull();
    }

    @Test
    void elMismoEventoRepetidoEnviaUnSoloCorreo() {
        UUID eventId = UUID.randomUUID();
        String payload = boletoVendido(ANA);
        publicarEvento(SALES_EVENTOS, "BoletoVendido", eventId, "2026-11-01", payload);
        publicarEvento(SALES_EVENTOS, "BoletoVendido", eventId, "2026-11-01", payload);
        // Marcador: la cola se consume en orden; cuando termina, el repetido ya pasó
        UUID marcador = publicarEvento(SALES_EVENTOS, "BoletoVendido", "2026-11-01",
                boletoVendido("otro@correo.com"));

        await().atMost(ESPERA).until(() -> procesado(marcador));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(totalCorreos()).isEqualTo(2));
        assertThat(asuntosPara(ANA)).hasSize(1);
        assertThat(notificaciones(eventId)).hasSize(1);
    }

    @Test
    void ventaEnMostradorSinCorreoNoRegistraNada() {
        UUID eventId = publicarEvento(SALES_EVENTOS, "BoletoVendido", "2026-11-01", boletoVendido(null));

        await().atMost(ESPERA).until(() -> procesado(eventId));

        assertThat(totalNotificaciones()).isZero();
        assertThat(totalCorreos()).isZero();
    }

    @Test
    void ventaCanceladaConCarteraAvisaElReembolso() {
        String payload = """
                {"ventaId":"%s","viajeId":"%s","sucursalId":"%s","clienteUsuarioId":"%s","clienteNit":"1234567-8",
                 "clienteNombre":"Ana López","clienteCorreo":"%s","asientos":[1,2],"cantidadBoletos":2,
                 "montoAnulado":150.00,"metodoPago":"CARTERA","motivo":"Viaje suspendido por lluvia",
                 "fechaCancelacion":"2026-11-03","origenNombre":"Quetzaltenango","destinoNombre":"Guatemala",
                 "fechaSalida":"2026-11-05T08:00:00-06:00"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ANA);

        UUID eventId = publicarEvento(SALES_EVENTOS, "VentaCancelada", "2026-11-03", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara(ANA)).containsExactly("Tu compra fue cancelada"));
        assertThat(textoPara(ANA)).contains("cancelada el 03/11/2026", "Viaje: Quetzaltenango → Guatemala",
                "Monto anulado: Q 150.00", "Motivo: Viaje suspendido por lluvia", "tu cartera digital");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "VENTA_CANCELADA");
    }

    @Test
    void ventaCanceladaDeViajeEliminadoSinDatosDelViaje() {
        String payload = """
                {"ventaId":"%s","viajeId":"%s","sucursalId":"%s","clienteNombre":"Ana","clienteCorreo":"%s",
                 "asientos":[7],"cantidadBoletos":1,"montoAnulado":75.00,"metodoPago":"EFECTIVO",
                 "motivo":"VIAJE_ELIMINADO","fechaCancelacion":"2026-11-03","origenNombre":null,
                 "destinoNombre":null,"fechaSalida":null}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ANA);

        UUID eventId = publicarEvento(SALES_EVENTOS, "VentaCancelada", "2026-11-03", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara(ANA)).hasSize(1));
        assertThat(textoPara(ANA)).doesNotContain("Viaje:").contains("Asientos: 7", "Acércate a la sucursal");
    }

    @Test
    void alquilerPagadoLeConfirmaAlClienteYNoLeEscribeAlAdmin() {
        String payload = """
                {"ordenId":"%s","alquilerId":"%s","sucursalId":"%s","clienteUsuarioId":null,"clienteNit":"CF",
                 "clienteNombre":"Colegio San José","clienteCorreo":"colegio@correo.com","origen":"Quetzaltenango",
                 "destino":"Antigua Guatemala","monto":2600.00,"montoTotal":2600.00,"metodoPago":"EFECTIVO",
                 "fechaPago":"2026-11-02"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        UUID eventId = publicarEvento(SALES_EVENTOS, "AlquilerPagado", "2026-11-02", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara("colegio@correo.com")).containsExactly("Pago de alquiler recibido"));
        assertThat(textoPara("colegio@correo.com")).contains("Recorrido: Quetzaltenango → Antigua Guatemala",
                "Monto: Q 2600.00, pagado con efectivo", "Fecha de pago: 02/11/2026");

        // el admin ya no recibe correo: el aviso de "pagos por asignar" vive en el menú del panel
        assertThat(asuntosPara(ADMIN_CORREO)).isEmpty();
        assertThat(notificaciones(eventId)).extracting(f -> f.get("plantilla"))
                .containsExactly("ALQUILER_PAGADO");
    }

    @Test
    void alquilerAsignadoAvisaAlClienteElBusYElPiloto() {
        String payload = """
                {"alquilerId":"%s","sucursalId":"%s","viajeId":"%s","clienteNombre":"Colegio San José",
                 "clienteCorreo":"colegio@correo.com","origen":"Quetzaltenango","destino":"Antigua Guatemala",
                 "fechaSalida":"2026-12-20T07:00:00-06:00","fechaRetorno":"2026-12-20T19:00:00-06:00",
                 "busId":"%s","busPlaca":"P-123ABC","choferId":"%s","choferNombre":"Juan Pérez"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID());

        UUID eventId = publicarEvento(TRIPS_EVENTOS, "AlquilerAsignado", "2026-12-20", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara("colegio@correo.com"))
                .containsExactly("Tu alquiler ya tiene bus y piloto"));
        assertThat(textoPara("colegio@correo.com")).contains("Recorrido: Quetzaltenango → Antigua Guatemala",
                "Salida: 20/12/2026 07:00", "Bus: P-123ABC", "Piloto: Juan Pérez");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "ALQUILER_ASIGNADO");
    }

    @Test
    void cotizacionConfirmadaEnviaElPrecio() {
        String payload = """
                {"alquilerId":"%s","sucursalId":"%s","canal":"EN_LINEA","clienteUsuarioId":"%s",
                 "clienteNit":"1234567-8","clienteNombre":"Colegio San José","clienteCorreo":"colegio@correo.com",
                 "origen":"Quetzaltenango","destino":"Antigua Guatemala",
                 "fechaSalida":"2026-12-20T07:00:00-06:00","fechaRetorno":"2026-12-20T19:00:00-06:00",
                 "cantidadPasajeros":30,"precioConfirmado":2600.00}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        UUID eventId = publicarEvento(TRIPS_EVENTOS, "CotizacionAlquilerConfirmada", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara("colegio@correo.com"))
                .containsExactly("Tu cotización de alquiler está lista"));
        assertThat(textoPara("colegio@correo.com")).contains("Salida: 20/12/2026 07:00",
                "Retorno: 20/12/2026 07:00", "Pasajeros: 30", "Precio: Q 2600.00");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "COTIZACION_ALQUILER");
    }
}