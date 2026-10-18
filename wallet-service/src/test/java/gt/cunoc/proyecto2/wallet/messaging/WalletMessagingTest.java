package gt.cunoc.proyecto2.wallet.messaging;

import gt.cunoc.proyecto2.comun.eventos.consumidor.ConsumidorEventos;
import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import gt.cunoc.proyecto2.comun.idempotencia.ProcesadorIdempotente;
import gt.cunoc.proyecto2.wallet.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.wallet.evento.recibido.CuentaCreada;
import gt.cunoc.proyecto2.wallet.evento.recibido.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.wallet.evento.recibido.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.wallet.services.DebitoCarteraService;
import gt.cunoc.proyecto2.wallet.services.ReembolsoCarteraService;
import gt.cunoc.proyecto2.wallet.services.WalletService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Recorrido listener -> consumer -> servicio -> outbox con repositorios e Identity simulados. */
class WalletMessagingTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ClienteIdentity identity = mock(ClienteIdentity.class);
    private final CarteraRepository carteras = mock(CarteraRepository.class);
    private final MovimientoRepository movimientos = mock(MovimientoRepository.class);
    private final WalletService wallet = mock(WalletService.class);
    private final Connection connection = mock(Connection.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID referenciaId = UUID.randomUUID();
    private final UUID eventoId = UUID.randomUUID();
    private final UUID correlacion = UUID.randomUUID();
    private final BigDecimal monto = new BigDecimal("35.50");
    private final LocalDate fechaNegocio = LocalDate.of(2026, 9, 20);
    private final LocalDate fechaPayload = LocalDate.of(2026, 9, 19);
    private AnnotationConfigApplicationContext contexto;

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class Transacciones {
    }

    @BeforeEach
    void preparar() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        contexto = new AnnotationConfigApplicationContext();
        contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("eventos", Map.of(
                "comun.eventos.exchange", "wallet.eventos", "comun.eventos.cola", "wallet.q")));
        contexto.registerBean(JdbcTemplate.class, () -> jdbc);
        contexto.registerBean(JsonMapper.class, () -> json);
        contexto.registerBean(ClienteIdentity.class, () -> identity);
        contexto.registerBean(CarteraRepository.class, () -> carteras);
        contexto.registerBean(MovimientoRepository.class, () -> movimientos);
        contexto.registerBean(WalletService.class, () -> wallet);
        contexto.registerBean("transactionManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(dataSource));
        contexto.register(Transacciones.class, PublicadorEventos.class, ProcesadorIdempotente.class,
                ConsumidorEventos.class, DebitoCarteraService.class, ReembolsoCarteraService.class);
        contexto.scan("gt.cunoc.proyecto2.wallet.messaging.consumer",
                "gt.cunoc.proyecto2.wallet.messaging.publisher");
        contexto.refresh();
    }

    @AfterEach
    void cerrar() {
        if (contexto != null) contexto.close();
        ContextoCorrelacion.limpiar();
    }

    @Test
    void registraLosTresConsumersSinDuplicados() {
        assertThat(contexto.getBeansOfType(ManejadorEvento.class).values())
                .extracting(ManejadorEvento::tipoEvento)
                .containsExactlyInAnyOrder("DebitoCarteraSolicitado", "ReembolsoCarteraSolicitado", "CuentaCreada");
    }

    // ----- Débito (saga) -----

    @Test
    void debitaYGuardaRespuestaConFechaDelSobreYCorrelacionEnUnaTransaccion() throws Exception {
        prepararDebito();
        MovimientoCartera movimiento = movimiento(monto);
        when(wallet.intentarDebitar(eq(usuarioId),
                argThat(valor -> valor != null && valor.compareTo(monto) == 0),
                eq("VENTA"), eq(referenciaId), eq(fechaNegocio)))
                .thenReturn(Optional.of(movimiento));

        recibirDebito();

        var sobre = salida("CarteraDebitada");
        assertThat(sobre.path("payload").path("movimientoId").asText()).isEqualTo(movimiento.getId().toString());
        assertThat(sobre.path("payload").path("monto").decimalValue()).isEqualByComparingTo(monto);
        assertThat(sobre.path("payload").path("fechaMovimiento").asText()).isEqualTo(fechaPayload.toString());
        verify(connection, times(1)).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void rechazaUsuarioInactivoSegunIdentity() {
        when(identity.cuentaActiva(usuarioId)).thenReturn(false);

        recibirDebito();

        assertThat(salida("DebitoCarteraRechazado").path("payload").path("motivo").asText())
                .isEqualTo("USUARIO_INACTIVO");
        verifyNoInteractions(carteras, movimientos, wallet);
    }

    @Test
    void rechazaCarteraInexistente() {
        when(identity.cuentaActiva(usuarioId)).thenReturn(true);

        recibirDebito();

        assertThat(salida("DebitoCarteraRechazado").path("payload").path("motivo").asText())
                .isEqualTo("CARTERA_INEXISTENTE");
        verifyNoInteractions(movimientos, wallet);
    }

    @Test
    void saldoInsuficienteEsUnResultadoNormalQueSeConfirma() throws Exception {
        prepararDebito();

        recibirDebito();

        assertThat(salida("DebitoCarteraRechazado").path("payload").path("motivo").asText())
                .isEqualTo("SALDO_INSUFICIENTE");
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void debitoConReferenciaExistenteRepublicaSinVolverADebitar() {
        prepararDebito();
        when(movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId))
                .thenReturn(Optional.of(movimiento(BigDecimal.ONE)));

        recibirDebito();

        assertThat(salida("CarteraDebitada").path("payload").path("monto").decimalValue())
                .isEqualByComparingTo(monto);
        verifyNoInteractions(wallet);
    }

    @Test
    void eventoDuplicadoNoEjecutaNegocioNiPublica() {
        when(jdbc.update(contains("INSERT INTO eventos_procesados"), eq(eventoId), eq("DebitoCarteraSolicitado")))
                .thenReturn(0);

        recibirDebito();

        verifyNoInteractions(identity, carteras, movimientos, wallet);
        sinSalida();
    }

    @Test
    void identityCaidoRevierteYPropagaParaReintentar() throws Exception {
        when(identity.cuentaActiva(usuarioId))
                .thenThrow(new gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException("IDENTITY_NO_DISPONIBLE", "x"));

        assertThatThrownBy(this::recibirDebito)
                .isInstanceOf(gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException.class);

        verify(connection).rollback();
        verify(connection, never()).commit();
        sinSalida();
    }

    @Test
    void errorAlGuardarOutboxRevierteLaTransaccionDelConsumoYPropagaElError() throws Exception {
        prepararDebito();
        when(wallet.intentarDebitar(eq(usuarioId),
                argThat(valor -> valor != null && valor.compareTo(monto) == 0),
                eq("VENTA"), eq(referenciaId), eq(fechaNegocio)))
                .thenReturn(Optional.of(movimiento(monto)));
        doThrow(new IllegalStateException("outbox no disponible"))
                .when(jdbc).update(contains("INSERT INTO eventos_salida"), any(Object[].class));

        assertThatThrownBy(this::recibirDebito).isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox no disponible");

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    // ----- Reembolso (compensación) -----

    @Test
    void reembolsoSinDebitoNoAcreditaNiPublica() {
        recibirReembolso();

        verifyNoInteractions(wallet);
        sinSalida();
    }

    @Test
    void reembolsoExistenteRepublicaElMontoPersistidoSinAcreditarOtraVez() {
        when(movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId))
                .thenReturn(Optional.of(movimiento(monto)));
        when(movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId))
                .thenReturn(Optional.of(movimiento(BigDecimal.TEN)));

        recibirReembolso();

        assertThat(salida("CarteraReembolsada").path("payload").path("monto").decimalValue())
                .isEqualByComparingTo(BigDecimal.TEN);
        verifyNoInteractions(wallet);
    }

    @Test
    void reembolsoNuevoConservaMotivoYFechaDelSobre() {
        when(movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId))
                .thenReturn(Optional.of(movimiento(monto)));
        when(wallet.solicitarReembolso(eq(usuarioId),
                argThat(valor -> valor != null && valor.compareTo(monto) == 0),
                eq("VENTA"), eq(referenciaId), eq("CANCELACION"), eq(fechaNegocio)))
                .thenReturn(Optional.of(movimiento(monto)));

        recibirReembolso();

        salida("CarteraReembolsada");
    }

    @Test
    void reembolsoSinMovimientoResultanteNoPublica() {
        when(movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId))
                .thenReturn(Optional.of(movimiento(monto)));

        recibirReembolso();

        sinSalida();
    }

    // ----- Alta de cartera -----

    @Test
    void cuentaDeClienteAbreLaCartera() {
        recibir("CuentaCreada", new CuentaCreada(usuarioId, List.of("CLIENTE")));

        verify(wallet).abrirCartera(usuarioId);
        sinSalida();
    }

    @Test
    void cuentaDePersonalOSinRolesNoAbreCartera() {
        recibir("CuentaCreada", new CuentaCreada(usuarioId, List.of("CAJERO")));
        recibir("CuentaCreada", new CuentaCreada(usuarioId, null));
        recibir("CuentaCreada", new CuentaCreada(null, List.of("CLIENTE")));

        verifyNoInteractions(wallet);
    }

    @Test
    void cuentaCreadaIgnoraLosDemasCamposDelPayloadDeIdentity() {
        // Mismo JSON que publica Identity: correo, nombre y urlActivacion no se usan
        recibir("CuentaCreada", Map.of("usuarioId", usuarioId.toString(), "correo", "ana@correo.com",
                "nombre", "Ana", "roles", List.of("CLIENTE")));

        verify(wallet).abrirCartera(usuarioId);
    }

    // ----- Soporte -----

    private void prepararDebito() {
        when(identity.cuentaActiva(usuarioId)).thenReturn(true);
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(new Cartera()));
    }

    private MovimientoCartera movimiento(BigDecimal importe) {
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setId(UUID.randomUUID());
        movimiento.setMonto(importe);
        movimiento.setSaldoResultante(new BigDecimal("100.00"));
        movimiento.setFechaMovimiento(fechaPayload);
        return movimiento;
    }

    private void recibirDebito() {
        recibir("DebitoCarteraSolicitado",
                new DebitoCarteraSolicitado("VENTA", referenciaId, usuarioId, monto, fechaPayload));
    }

    private void recibirReembolso() {
        recibir("ReembolsoCarteraSolicitado",
                new ReembolsoCarteraSolicitado("VENTA", referenciaId, usuarioId, monto, "CANCELACION", fechaPayload));
    }

    private void recibir(String tipo, Object payload) {
        EventoSobre sobre = new EventoSobre(eventoId, tipo, 1,
                usuarioId, null, Instant.now(), fechaNegocio, correlacion, payload);
        contexto.getBean(ConsumidorEventos.class).recibir(
                new Message(json.writeValueAsBytes(sobre), new MessageProperties()));
    }

    private tools.jackson.databind.JsonNode salida(String tipo) {
        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(contains("INSERT INTO eventos_salida"), any(UUID.class), eq("Cartera"),
                eq(usuarioId), eq(tipo), eq(1), datos.capture());
        var sobre = json.readTree((String) datos.getValue());
        assertThat(sobre.path("eventType").asText()).isEqualTo(tipo);
        assertThat(sobre.path("correlationId").asText()).isEqualTo(correlacion.toString());
        assertThat(sobre.path("businessDate").asText()).isEqualTo(fechaNegocio.toString());
        assertThat(sobre.path("payload").path("referenciaId").asText()).isEqualTo(referenciaId.toString());
        assertThat(sobre.path("payload").path("usuarioId").asText()).isEqualTo(usuarioId.toString());
        return sobre;
    }

    private void sinSalida() {
        verify(jdbc, never()).update(contains("INSERT INTO eventos_salida"), any(Object[].class));
    }
}
