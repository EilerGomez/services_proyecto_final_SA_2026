package gt.cunoc.proyecto2.identity.messaging;

import gt.cunoc.proyecto2.comun.eventos.consumidor.ConsumidorEventos;
import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import gt.cunoc.proyecto2.comun.idempotencia.ProcesadorIdempotente;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferCreado;
import gt.cunoc.proyecto2.identity.evento.recibido.ChoferDesactivado;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalAsignado;
import gt.cunoc.proyecto2.identity.evento.recibido.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRefresco;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.personal.SincronizacionPersonalService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Recorrido listener -> consumer -> servicio con repositorios simulados (sincronización de personal). */
class IdentityMessagingTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final TokenRefrescoRepository refrescos = mock(TokenRefrescoRepository.class);
    private final PerfilRepository perfiles = mock(PerfilRepository.class);
    private final Connection connection = mock(Connection.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sucursalId = UUID.randomUUID();
    private final UUID eventoId = UUID.randomUUID();
    private final UUID correlacion = UUID.randomUUID();
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
        contexto.registerBean(RolRepository.class, () -> roles);
        contexto.registerBean(TokenRefrescoRepository.class, () -> refrescos);
        contexto.registerBean(PerfilRepository.class, () -> perfiles);
        contexto.registerBean("transactionManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(dataSource));
        contexto.register(Transacciones.class, PublicadorEventos.class, ProcesadorIdempotente.class,
                ConsumidorEventos.class, SincronizacionPersonalService.class);
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
    void registraLosConsumersSinDuplicados() {
        // La saga de cartera (DebitoCarteraSolicitado, ReembolsoCarteraSolicitado) ahora la atiende wallet-service
        assertThat(contexto.getBeansOfType(ManejadorEvento.class).values())
                .extracting(ManejadorEvento::tipoEvento)
                .containsExactlyInAnyOrder("ChoferCreado", "ChoferDesactivado", "PersonalSucursalAsignado",
                        "PersonalSucursalDesactivado");
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
        // Rol de personal nuevo para la cuenta: se publica RolAsignado (aviso por correo)
        boolean avisa = codigo.equals(Roles.CAJERO) || codigo.equals(Roles.ADMIN_SUCURSAL);
        verify(jdbc, avisa ? times(1) : never()).update(contains("INSERT INTO eventos_salida"), any(Object[].class));
    }

    @Test
    void asignacionConElRolYaPuestoNoVuelveAAvisar() {
        // Alta por R5 (cuenta nueva): Identity ya puso el rol antes de que llegue el evento de Fleet
        Usuario usuario = usuario();
        Rol cajero = rol(Roles.CAJERO);
        usuario.getRoles().add(rol(Roles.CAJERO));
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(roles.findById(Roles.CAJERO)).thenReturn(Optional.of(cajero));
        recibir("PersonalSucursalAsignado", new PersonalSucursalAsignado(
                UUID.randomUUID(), usuarioId, sucursalId, Roles.CAJERO, fechaPayload, "Xela Central"));
        assertThat(usuario.getSucursalId()).isEqualTo(sucursalId);
        verify(usuarios).save(usuario);
        sinSalida();
    }

    @Test
    void unAdminDelSistemaNoRecibeRolDePersonal() {
        // Lo promovieron entre la validación de R5b y la llegada del evento
        Usuario usuario = usuario();
        usuario.getRoles().add(rol(Roles.ADMIN_SISTEMA));
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        recibir("PersonalSucursalAsignado", new PersonalSucursalAsignado(
                UUID.randomUUID(), usuarioId, sucursalId, Roles.CAJERO, fechaPayload, "Xela Central"));
        assertThat(usuario.getSucursalId()).isNull();
        assertThat(usuario.getRoles()).extracting(Rol::getCodigo).containsExactly(Roles.ADMIN_SISTEMA);
        verify(usuarios, never()).save(any());
        verifyNoInteractions(roles);
        sinSalida();
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
    void choferConCuentaExistenteRecibeElRolLaSucursalYElAviso() {
        Usuario usuario = usuario();
        usuario.getRoles().add(rol(Roles.CLIENTE));
        UUID choferId = UUID.randomUUID();
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(roles.findById(Roles.CHOFER)).thenReturn(Optional.of(rol(Roles.CHOFER)));

        recibir("ChoferCreado", new ChoferCreado(choferId, usuarioId, sucursalId));

        assertThat(usuario.getChoferId()).isEqualTo(choferId);
        assertThat(usuario.getSucursalId()).isEqualTo(sucursalId);
        assertThat(usuario.getRoles()).extracting(Rol::getCodigo)
                .containsExactlyInAnyOrder(Roles.CLIENTE, Roles.CHOFER);
        verify(usuarios).save(usuario);
        verify(jdbc).update(contains("INSERT INTO eventos_salida"), any(Object[].class));   // RolAsignado
    }

    @Test
    void choferConCuentaNuevaYaTieneElRolYNoSeVuelveAAvisar() {
        Usuario usuario = usuario();
        UUID choferId = UUID.randomUUID();
        usuario.setChoferId(choferId);
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));

        recibir("ChoferCreado", new ChoferCreado(choferId, usuarioId, sucursalId));

        verify(usuarios, never()).save(any());
        verifyNoInteractions(roles);
        sinSalida();
    }

    @ParameterizedTest
    @ValueSource(strings = {Roles.ADMIN_SISTEMA, Roles.CAJERO})
    void choferNoSeAsignaSiLaCuentaPasoASerAdminOPersonal(String codigo) {
        Usuario usuario = usuario();
        usuario.getRoles().add(rol(codigo));
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));

        recibir("ChoferCreado", new ChoferCreado(UUID.randomUUID(), usuarioId, sucursalId));

        assertThat(usuario.getChoferId()).isNull();
        verify(usuarios, never()).save(any());
        sinSalida();
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

    private void sinSalida() {
        verify(jdbc, never()).update(contains("INSERT INTO eventos_salida"), any(Object[].class));
    }
}
