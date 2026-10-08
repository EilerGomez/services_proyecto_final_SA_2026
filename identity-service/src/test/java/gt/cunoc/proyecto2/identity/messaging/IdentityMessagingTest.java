package gt.cunoc.proyecto2.identity.messaging;

import gt.cunoc.proyecto2.comun.eventos.consumidor.ConsumidorEventos;
import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import gt.cunoc.proyecto2.comun.idempotencia.ProcesadorIdempotente;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferDesactivado;
import gt.cunoc.proyecto2.identity.evento.recibido.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalAsignado;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.identity.evento.recibido.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRefresco;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.cartera.DebitoCarteraService;
import gt.cunoc.proyecto2.identity.services.cartera.ReembolsoCarteraService;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
import gt.cunoc.proyecto2.identity.services.personal.SincronizacionPersonalService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Prueba el recorrido listener -> consumer -> servicio -> outbox con repositorios simulados. */
class IdentityMessagingTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CarteraRepository carteras = mock(CarteraRepository.class);
    private final MovimientoRepository movimientos = mock(MovimientoRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final TokenRefrescoRepository refrescos = mock(TokenRefrescoRepository.class);
    private final WalletService wallet = mock(WalletService.class);
    private final Connection connection = mock(Connection.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID referenciaId = UUID.randomUUID();
    private final UUID sucursalId = UUID.randomUUID();
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
                "comun.eventos.exchange", "identity.eventos", "comun.eventos.cola", "identity.q")));
        contexto.registerBean(JdbcTemplate.class, () -> jdbc);
        contexto.registerBean(JsonMapper.class, () -> json);
        contexto.registerBean(UsuarioRepository.class, () -> usuarios);
        contexto.registerBean(CarteraRepository.class, () -> carteras);
        contexto.registerBean(MovimientoRepository.class, () -> movimientos);
        contexto.registerBean(RolRepository.class, () -> roles);
        contexto.registerBean(TokenRefrescoRepository.class, () -> refrescos);
        contexto.registerBean(WalletService.class, () -> wallet);
        contexto.registerBean("transactionManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(dataSource));
        contexto.register(Transacciones.class, PublicadorEventos.class, ProcesadorIdempotente.class,
                ConsumidorEventos.class, DebitoCarteraService.class, ReembolsoCarteraService.class,
                SincronizacionPersonalService.class);
        contexto.scan("gt.cunoc.proyecto2.identity.messaging.consumer",
                "gt.cunoc.proyecto2.identity.messaging.publisher");
        contexto.refresh();
    }

    @AfterEach
    void cerrar() {
        if (contexto != null) contexto.close();
        ContextoCorrelacion.limpiar();
    }

    @Test
    void registraLosCincoConsumersSinDuplicados() {
        assertThat(contexto.getBeansOfType(ManejadorEvento.class).values())
                .extracting(ManejadorEvento::tipoEvento)
                .containsExactlyInAnyOrder("DebitoCarteraSolicitado", "ReembolsoCarteraSolicitado",
                        "ChoferDesactivado", "PersonalSucursalAsignado", "PersonalSucursalDesactivado");
    }

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
        verify(wallet).intentarDebitar(eq(usuarioId),
                argThat(valor -> valor != null && valor.compareTo(monto) == 0),
                eq("VENTA"), eq(referenciaId), eq(fechaNegocio));
        verify(connection, times(1)).commit();
        verify(connection, never()).rollback();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUSENTE", "INACTIVO"})
    void rechazaUsuarioAusenteOInactivo(String estado) {
        if (!estado.equals("AUSENTE")) {
            Usuario usuario = usuario();
            usuario.setEstado(estado);
            when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        }
        recibirDebito();
        assertThat(salida("DebitoCarteraRechazado").path("payload").path("motivo").asText())
                .isEqualTo("USUARIO_INACTIVO");
        verifyNoInteractions(carteras, movimientos, wallet);
    }

    @Test
    void rechazaCarteraInexistente() {
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario()));
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
        verifyNoInteractions(usuarios, carteras, movimientos, wallet);
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
        verify(wallet).solicitarReembolso(eq(usuarioId),
                argThat(valor -> valor != null && valor.compareTo(monto) == 0),
                eq("VENTA"), eq(referenciaId), eq("CANCELACION"), eq(fechaNegocio));
    }

    @Test
    void reembolsoSinMovimientoResultanteNoPublica() {
        when(movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId))
                .thenReturn(Optional.of(movimiento(monto)));
        recibirReembolso();
        sinSalida();
    }

    @ParameterizedTest
    @ValueSource(strings = {Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA, "NO_EXISTE"})
    void asignacionSoloAgregaRolesPermitidos(String codigo) {
        Usuario usuario = usuario();
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        if (!codigo.equals("NO_EXISTE")) when(roles.findById(codigo)).thenReturn(Optional.of(rol(codigo)));
        recibir("PersonalSucursalAsignado", new PersonalSucursalAsignado(
                UUID.randomUUID(), usuarioId, sucursalId, codigo, fechaPayload));
        assertThat(usuario.getSucursalId()).isEqualTo(sucursalId);
        if (codigo.equals(Roles.CAJERO) || codigo.equals(Roles.ADMIN_SUCURSAL)) {
            assertThat(usuario.getRoles()).extracting(Rol::getCodigo).containsExactly(codigo);
        } else {
            assertThat(usuario.getRoles()).isEmpty();
        }
        verify(usuarios).save(usuario);
    }

    @Test
    void bajaDePersonalEnOtraSucursalSeIgnora() {
        Usuario usuario = usuario();
        UUID otraSucursal = UUID.randomUUID();
        usuario.setSucursalId(otraSucursal);
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        recibirBajaPersonal();
        assertThat(usuario.getSucursalId()).isEqualTo(otraSucursal);
        verify(usuarios, never()).save(any());
        verifyNoInteractions(refrescos);
    }

    @Test
    void bajaDePersonalRetiraSoloSuRolYRevocaSesiones() {
        Usuario usuario = usuario();
        usuario.setSucursalId(sucursalId);
        usuario.getRoles().addAll(List.of(rol(Roles.CAJERO), rol(Roles.CLIENTE)));
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        TokenRefresco token = new TokenRefresco();
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuarioId)).thenReturn(List.of(token));
        recibirBajaPersonal();
        assertThat(usuario.getSucursalId()).isNull();
        assertThat(usuario.getRoles()).extracting(Rol::getCodigo).containsExactly(Roles.CLIENTE);
        assertThat(token.getRevocadoEn()).isNotNull();
        verify(refrescos).save(token);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bajaChoferConservaHistorialYCuentaClienteAunqueLaSucursalSeaDistinta(boolean mismaSucursal) {
        Usuario usuario = usuario();
        UUID choferId = UUID.randomUUID();
        UUID sucursalActual = mismaSucursal ? sucursalId : UUID.randomUUID();
        usuario.setSucursalId(sucursalActual);
        usuario.setChoferId(choferId);
        usuario.getRoles().addAll(List.of(rol(Roles.CHOFER), rol(Roles.CLIENTE)));
        when(usuarios.findByChoferId(choferId)).thenReturn(Optional.of(usuario));
        TokenRefresco token = new TokenRefresco();
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuarioId)).thenReturn(List.of(token));
        recibir("ChoferDesactivado", new ChoferDesactivado(choferId, usuarioId, sucursalId));
        assertThat(usuario.getSucursalId()).isEqualTo(mismaSucursal ? null : sucursalActual);
        assertThat(usuario.getChoferId()).isEqualTo(choferId);
        assertThat(usuario.getEstado()).isEqualTo("ACTIVO");
        assertThat(usuario.getRoles()).extracting(Rol::getCodigo).containsExactly(Roles.CLIENTE);
        assertThat(token.getRevocadoEn()).isNotNull();
        verify(usuarios).save(usuario);
    }

    @Test
    void eventosDePersonalSinUsuarioNoModificanNada() {
        recibir("PersonalSucursalAsignado", new PersonalSucursalAsignado(
                UUID.randomUUID(), usuarioId, sucursalId, Roles.CAJERO, fechaPayload));
        recibirBajaPersonal();
        recibir("ChoferDesactivado", new ChoferDesactivado(UUID.randomUUID(), usuarioId, sucursalId));
        verify(usuarios, never()).save(any());
        verifyNoInteractions(roles, refrescos);
        sinSalida();
    }

    private void prepararDebito() {
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario()));
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(new Cartera()));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        return usuario;
    }

    private Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
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

    private void recibirBajaPersonal() {
        recibir("PersonalSucursalDesactivado", new PersonalSucursalDesactivado(
                UUID.randomUUID(), usuarioId, sucursalId, Roles.CAJERO, fechaPayload));
    }

    private void recibir(String tipo, Object payload) {
        EventoSobre sobre = new EventoSobre(eventoId, tipo, 1, usuarioId, null,
                Instant.now(), fechaNegocio, correlacion, payload);
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
