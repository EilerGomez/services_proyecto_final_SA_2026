package gt.cunoc.proyecto2.wallet.integracion.cartera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.wallet.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Cartera propia: alta, saldo, recargas por la pasarela simulada e historial con filtros. */
class CarteraIT extends PruebaIntegracion {

    @Test
    void primerAccesoAbreLaCarteraEnCero() {
        UUID usuarioId = UUID.randomUUID();

        ResponseEntity<String> saldo = get("/wallet/cartera", tokenCliente(usuarioId));

        assertThat(saldo.getStatusCode().value()).as(saldo.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<Double>json(saldo, "$.saldo")).isEqualTo(0.0);
        assertThat(contar("carteras")).isEqualTo(1);
    }

    @Test
    void recargarSumaGuardaLaPasarelaYApareceEnHistorial() {
        UUID usuarioId = UUID.randomUUID();
        String token = tokenCliente(usuarioId);

        ResponseEntity<String> recarga = post("/wallet/cartera/recargas", token,
                "{\"monto\":200.00,\"fechaMovimiento\":\"2026-10-20\"}");
        assertThat(recarga.getStatusCode().value()).as(recarga.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(recarga, "$.fechaMovimiento")).isEqualTo("2026-10-20");

        ResponseEntity<String> saldo = get("/wallet/cartera", token);
        assertThat(PruebaIntegracion.<Double>json(saldo, "$.saldo")).isEqualTo(200.00);

        ResponseEntity<String> historial = get(
                "/wallet/cartera/movimientos?desde=2026-10-01&hasta=2026-10-31", token);
        assertThat(PruebaIntegracion.<List<String>>json(historial, "$[*].tipo")).contains("RECARGA");
        assertThat(jdbc.queryForObject("SELECT proveedor_pago FROM movimientos_cartera", String.class))
                .isEqualTo("SIMULADO");
    }

    @Test
    void elHistorialMuestraPrimeroLoMasRecienteAunqueSeaDelMismoDia() {
        String token = tokenCliente(UUID.randomUUID());
        post("/wallet/cartera/recargas", token, "{\"monto\":10.00,\"fechaMovimiento\":\"2026-10-20\"}");
        post("/wallet/cartera/recargas", token, "{\"monto\":20.00,\"fechaMovimiento\":\"2026-10-21\"}");
        post("/wallet/cartera/recargas", token, "{\"monto\":30.00,\"fechaMovimiento\":\"2026-10-21\"}");

        ResponseEntity<String> historial = get(
                "/wallet/cartera/movimientos?desde=2026-10-01&hasta=2026-10-31", token);

        // 21/10 la de 30 (registrada después), 21/10 la de 20, 20/10 la de 10
        assertThat(PruebaIntegracion.<List<Double>>json(historial, "$[*].monto")).containsExactly(30.0, 20.0, 10.0);
        assertThat(PruebaIntegracion.<List<Double>>json(historial, "$[*].saldoResultante"))
                .containsExactly(60.0, 30.0, 10.0);
        assertThat(PruebaIntegracion.<List<String>>json(historial, "$[*].fechaRegistro")).doesNotContainNull();
    }

    @Test
    void elHistorialSinFiltroDeFechasDevuelveTodosLosMovimientos() {
        String token = tokenCliente(UUID.randomUUID());
        post("/wallet/cartera/recargas", token, "{\"monto\":10.00,\"fechaMovimiento\":\"2026-10-20\"}");
        post("/wallet/cartera/recargas", token, "{\"monto\":20.00,\"fechaMovimiento\":\"2027-03-15\"}");

        // Sin desde ni hasta no hay tope: también sale el movimiento con fecha futura
        ResponseEntity<String> historial = get("/wallet/cartera/movimientos", token);

        assertThat(historial.getStatusCode().value()).as(historial.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<List<Double>>json(historial, "$[*].monto")).containsExactly(20.0, 10.0);
    }

    @Test
    void laRecargaSinFechaSeGuardaConLaDeHoy() {
        ResponseEntity<String> recarga = post("/wallet/cartera/recargas", tokenCliente(UUID.randomUUID()),
                "{\"monto\":50.00}");

        assertThat(recarga.getStatusCode().value()).as(recarga.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(recarga, "$.fechaMovimiento"))
                .isEqualTo(LocalDate.now().toString());
    }

    @Test
    void laRecargaQueSuperaElLimiteSeRechazaSinCobrarla() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "9999999999.99");

        // Se valida antes de llamar a la pasarela: no debe quedar un segundo movimiento
        esError(post("/wallet/cartera/recargas", tokenCliente(usuarioId),
                "{\"monto\":0.01,\"fechaMovimiento\":\"2026-10-20\"}"), 422, "SALDO_EXCEDE_LIMITE");
        assertThat(contar("movimientos_cartera")).isEqualTo(1);
    }

    @Test
    void recargaSinMontoSeRechaza() {
        esError(post("/wallet/cartera/recargas", tokenCliente(UUID.randomUUID()),
                "{\"monto\":0,\"fechaMovimiento\":\"2026-10-20\"}"), 400, "VALIDACION");
    }

    @Test
    void sinTokenOConOtroRolNoSeEntra() {
        assertThat(get("/wallet/cartera", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/wallet/cartera", tokenCajero(UUID.randomUUID())).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void cuentaCreadaDeClienteAbreLaCartera() {
        UUID cliente = UUID.randomUUID();
        UUID cajero = UUID.randomUUID();

        publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", cliente, """
                {"usuarioId":"%s","correo":"ana@pruebas.gt","nombre":"Ana","roles":["CLIENTE"],"urlActivacion":null}
                """.formatted(cliente));
        publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", cajero, """
                {"usuarioId":"%s","correo":"caja@pruebas.gt","nombre":"Caja","roles":["CAJERO"],"urlActivacion":null}
                """.formatted(cajero));

        await().atMost(Duration.ofSeconds(10)).until(() -> contar("eventos_procesados") == 2);
        assertThat(jdbc.queryForList("SELECT usuario_id FROM carteras", UUID.class)).containsExactly(cliente);
    }

    @Test
    void cuentaCreadaRepetidaOIncompletaNoDuplicaNiRompeLaCartera() {
        UUID cliente = UUID.randomUUID();
        String payload = """
                {"usuarioId":"%s","correo":"ana@pruebas.gt","nombre":"Ana","roles":["CLIENTE"],"urlActivacion":null}
                """.formatted(cliente);

        // Identity reintentó: el mismo cliente dos veces (con otro eventId cada vez)
        publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", cliente, payload);
        publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", cliente, payload);
        // Y dos eventos incompletos que no deben tumbar el consumidor
        publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", UUID.randomUUID(), """
                {"usuarioId":null,"correo":"sin@pruebas.gt","nombre":"Sin cuenta","roles":["CLIENTE"]}
                """);
        publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", UUID.randomUUID(), """
                {"usuarioId":"%s","correo":"sin@pruebas.gt","nombre":"Sin rol","roles":null}
                """.formatted(UUID.randomUUID()));

        await().atMost(Duration.ofSeconds(10)).until(() -> contar("eventos_procesados") == 4);
        assertThat(jdbc.queryForList("SELECT usuario_id FROM carteras", UUID.class)).containsExactly(cliente);
    }
}
