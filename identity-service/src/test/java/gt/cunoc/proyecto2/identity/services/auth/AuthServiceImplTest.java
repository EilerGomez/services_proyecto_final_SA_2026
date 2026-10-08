package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.VerificarDobleFactorRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.CodigoDobleFactor;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.TokenRefresco;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.CodigoDobleFactorRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.IdentidadUsuarioFactory;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;
    @Mock private CarteraRepository carteras;
    @Mock private TokenRefrescoRepository refrescos;
    @Mock private TokenRecuperacionRepository recuperaciones;
    @Mock private CodigoDobleFactorRepository desafios;
    @Mock private ServicioJwt jwt;
    @Mock private UsuarioEventPublisher publicador;
    @Mock private UserService servicioUsuarios;

    // Espías con comportamiento real: el hash debe verificarse de verdad y
    // el SHA-256 es determinista para predecir los hashes guardados.
    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();
    @Spy private TokenOpacoService opacos = new TokenOpacoService();

    private AuthServiceImpl servicio;

    @BeforeEach
    void preparar() {
        // Constructor manual: Mockito no inyecta primitivos ni String (minutosAcceso, urlFrontend).
        servicio = new AuthServiceImpl(usuarios, perfiles, roles, carteras, refrescos,
                recuperaciones, desafios, claves, jwt, 60L, new IdentidadUsuarioFactory(),
                opacos, publicador, servicioUsuarios, "http://localhost:5173");
    }

    @Test
    void iniciarSesionOkEmiteTokensYLimpiaIntentos() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt-test");

        // Act
        SesionResponse respuesta = (SesionResponse) servicio.iniciarSesion(
                new InicioSesionRequest("ana@correo.com", "Segura#2026"));

        // Assert
        assertAll(
                () -> assertEquals("jwt-test", respuesta.tokenAcceso()),
                () -> assertNotNull(respuesta.tokenRefresco()),
                () -> assertEquals(0, usuario.getIntentosFallidos()),
                () -> assertNotNull(usuario.getUltimoAcceso()),
                () -> verify(refrescos).save(any(TokenRefresco.class)));
    }

    @Test
    void credencialesMalasCuentanIntento() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

        // Act
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.iniciarSesion(new InicioSesionRequest("ana@correo.com", "otra")));

        // Assert
        assertAll(
                () -> assertEquals("CREDENCIALES_INVALIDAS", error.getCodigo()),
                () -> assertEquals(3, usuario.getIntentosFallidos()),
                () -> verify(usuarios).save(usuario));
    }

    @Test
    void quintoFalloBloqueaLaCuenta() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.setIntentosFallidos(4);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

        // Act
        assertThrows(NoAutenticadoException.class,
                () -> servicio.iniciarSesion(new InicioSesionRequest("ana@correo.com", "otra")));

        // Assert
        assertAll(
                () -> assertEquals("BLOQUEADO", usuario.getEstado()),
                () -> assertNotNull(usuario.getBloqueadoHasta()));
    }

    @Test
    void loginConDobleFactorDevuelveDesafioYPideCodigoPorCorreo() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<String> codigoEnviado = ArgumentCaptor.forClass(String.class);

        // Act
        var resultado = servicio.iniciarSesion(new InicioSesionRequest("ana@correo.com", "Segura#2026"));

        // Assert
        assertAll(
                () -> assertInstanceOf(
                        gt.cunoc.proyecto2.identity.dtos.auth.DesafioDobleFactor.class, resultado),
                () -> verify(publicador).publicarCodigoDobleFactor(
                        eq(USUARIO_ID), eq("ana@correo.com"), any(), codigoEnviado.capture(), any(), any()),
                () -> verify(jwt, never()).generar(any()));
    }

    @Test
    void verificarDobleFactorOkEmiteTokens() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<String> evento = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CodigoDobleFactor> guardado = ArgumentCaptor.forClass(CodigoDobleFactor.class);
        servicio.iniciarSesion(new InicioSesionRequest("ana@correo.com", "Segura#2026"));
        verify(publicador).publicarCodigoDobleFactor(
                eq(USUARIO_ID), any(), any(), evento.capture(), any(), any());
        verify(desafios).save(guardado.capture());
        when(desafios.findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(USUARIO_ID))
                .thenReturn(List.of(guardado.getValue()));
        String codigo = evento.getValue();
        when(jwt.generar(any())).thenReturn("jwt-test");

        // Act
        SesionResponse respuesta = servicio.verificarDobleFactor(
                new VerificarDobleFactorRequest("ana@correo.com", codigo));

        // Assert
        assertAll(
                () -> assertEquals("jwt-test", respuesta.tokenAcceso()),
                () -> verify(desafios, org.mockito.Mockito.times(2))
                        .save(any(gt.cunoc.proyecto2.identity.models.CodigoDobleFactor.class)));
    }

    @Test
    void codigoReutilizadoSeRechaza() {
        // Arrange: no hay desafío pendiente (ya se usó o nunca existió)
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(desafios.findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(USUARIO_ID))
                .thenReturn(List.of());

        // Act
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.verificarDobleFactor(new VerificarDobleFactorRequest("ana@correo.com", "123456")));

        // Assert
        assertEquals("DOBLE_FACTOR_INVALIDO", error.getCodigo());
    }

    @Test
    void refrescarRotaElTokenAnterior() {
        // Arrange
        Usuario usuario = usuarioActivo();
        TokenRefresco guardado = new TokenRefresco();
        guardado.setUsuarioId(USUARIO_ID);
        guardado.setExpiraEn(Instant.now().plusSeconds(3600));
        when(refrescos.findByTokenHash(opacos.calcularHash("crudo")))
                .thenReturn(Optional.of(guardado));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt-nuevo");

        // Act
        SesionResponse respuesta = servicio.renovarSesion(new TokenRefrescoRequest("crudo"));

        // Assert
        assertAll(
                () -> assertEquals("jwt-nuevo", respuesta.tokenAcceso()),
                () -> assertNotNull(guardado.getRevocadoEn()),
                () -> verify(refrescos, org.mockito.Mockito.times(2)).save(any(TokenRefresco.class)));
    }

    @Test
    void registroDuplicadoNoGuardaNada() {
        // Arrange
        when(usuarios.existsByCorreo("ana@correo.com")).thenReturn(true);

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ConflictoException.class,
                () -> servicio.registrar(new RegistroRequest("ana@correo.com", "Segura#2026",
                        "Ana", "López", "1234567-8", "1234567890101", "55551234", "Xela", null)));

        // Assert
        assertAll(
                () -> assertEquals("CORREO_EN_USO", error.getCodigo()),
                () -> verify(usuarios, never()).save(any()));
    }

    @Test
    void solicitarRecuperacionPublicaEnlace() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

        // Act
        servicio.solicitarRecuperacion(
                new gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest("ana@correo.com"));

        // Assert
        verify(publicador).publicarRecuperacionContrasenaSolicitada(
                eq(USUARIO_ID), eq("ana@correo.com"), any(), any(), any(), any());
        verify(recuperaciones).save(any(TokenRecuperacion.class));
    }

    @Test
    void recuperarCorreoInexistenteNoPublica() {
        // Arrange
        when(usuarios.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        // Act
        servicio.solicitarRecuperacion(
                new gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest("nadie@correo.com"));

        // Assert
        verify(publicador, never()).publicarRecuperacionContrasenaSolicitada(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void restablecerContrasenaRevocaSesiones() {
        // Arrange
        Usuario usuario = usuarioActivo();
        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuarioId(USUARIO_ID);
        token.setExpiraEn(Instant.now().plusSeconds(600));
        when(recuperaciones.findByTokenHash(opacos.calcularHash("crudo")))
                .thenReturn(Optional.of(token));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        TokenRefresco sesion = new TokenRefresco();
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(USUARIO_ID))
                .thenReturn(java.util.List.of(sesion));

        // Act
        servicio.restablecerContrasena(
                new gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest("crudo", "Nueva#2026"));

        // Assert
        assertAll(
                () -> assertTrue(claves.matches("Nueva#2026", usuario.getClaveCifrada())),
                () -> assertNotNull(token.getUsadoEn()),
                () -> assertNotNull(sesion.getRevocadoEn()));
    }

    @Test
    void restablecerTokenUsadoDa422() {
        // Arrange
        TokenRecuperacion token = new TokenRecuperacion();
        token.setExpiraEn(Instant.now().plusSeconds(600));
        token.setUsadoEn(Instant.now());
        when(recuperaciones.findByTokenHash(opacos.calcularHash("crudo")))
                .thenReturn(Optional.of(token));

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ReglaNegocioException.class,
                () -> servicio.restablecerContrasena(
                        new gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest("crudo", "Nueva#2026")));

        // Assert
        assertEquals("TOKEN_USADO", error.getCodigo());
    }

    @Test
    void cambiarContrasenaConActualMalaDa422() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ReglaNegocioException.class,
                () -> servicio.cambiarContrasena(USUARIO_ID,
                        new gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest(
                                "otra", "Nueva#2026")));

        // Assert
        assertEquals("CONTRASENA_ACTUAL_INCORRECTA", error.getCodigo());
    }

    @Test
    void cerrarSesionRevocaElRefresco() {
        // Arrange
        TokenRefresco guardado = new TokenRefresco();
        guardado.setUsuarioId(USUARIO_ID);
        when(refrescos.findByTokenHash(opacos.calcularHash("crudo")))
                .thenReturn(Optional.of(guardado));

        // Act
        servicio.cerrarSesion(USUARIO_ID, new TokenRefrescoRequest("crudo"));

        // Assert
        assertAll(
                () -> assertNotNull(guardado.getRevocadoEn()),
                () -> verify(refrescos).save(guardado));
    }

    @Test
    void loginBloqueoVencidoReactivaYEmiteTokens() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.setEstado("BLOQUEADO");
        usuario.setBloqueadoHasta(Instant.now().minusSeconds(60));
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt-test");

        // Act
        SesionResponse respuesta = (SesionResponse) servicio.iniciarSesion(
                new InicioSesionRequest("ana@correo.com", "Segura#2026"));

        // Assert
        assertAll(
                () -> assertEquals("jwt-test", respuesta.tokenAcceso()),
                () -> assertEquals("ACTIVO", usuario.getEstado()));
    }

    @Test
    void loginCuentaInactivaDa401() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.setEstado("INACTIVO");
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));

        // Act
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.iniciarSesion(new InicioSesionRequest("ana@correo.com", "Segura#2026")));

        // Assert
        assertEquals("CUENTA_INACTIVA", error.getCodigo());
    }

    @Test
    void renovarSesionRevocadaDa401() {
        // Arrange
        TokenRefresco guardado = new TokenRefresco();
        guardado.setUsuarioId(USUARIO_ID);
        guardado.setRevocadoEn(Instant.now());
        guardado.setExpiraEn(Instant.now().plusSeconds(3600));
        when(refrescos.findByTokenHash(opacos.calcularHash("crudo")))
                .thenReturn(Optional.of(guardado));

        // Act
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.renovarSesion(new TokenRefrescoRequest("crudo")));

        // Assert
        assertEquals("TOKEN_REFRESCO_INVALIDO", error.getCodigo());
    }

    @Test
    void renovarSesionInexistenteDa401() {
        // Arrange
        when(refrescos.findByTokenHash(opacos.calcularHash("crudo"))).thenReturn(Optional.empty());

        // Act
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.renovarSesion(new TokenRefrescoRequest("crudo")));

        // Assert
        assertEquals("TOKEN_REFRESCO_INVALIDO", error.getCodigo());
    }

    @Test
    void cambiarContrasenaOkActualizaHash() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        // Act
        servicio.cambiarContrasena(USUARIO_ID,
                new gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest("Segura#2026", "Nueva#2026"));

        // Assert
        assertAll(
                () -> assertTrue(claves.matches("Nueva#2026", usuario.getClaveCifrada())),
                () -> verify(usuarios).save(usuario));
    }

    @Test
    void miPerfilDelegaEnUsuarios() {
        // Arrange
        var perfil = new gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse(
                USUARIO_ID, "ana@correo.com", "Ana", "López", "1234567-8", "1234567890101",
                "55551234", "Xela", null, Set.of(Roles.CLIENTE), null, null);
        when(servicioUsuarios.perfil(USUARIO_ID)).thenReturn(perfil);

        // Act
        var respuesta = servicio.miPerfil(USUARIO_ID);

        // Assert
        assertEquals("ana@correo.com", respuesta.correo());
    }

    private Usuario usuarioActivo() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setCorreo("ana@correo.com");
        usuario.setClaveCifrada(claves.encode("Segura#2026"));
        usuario.setEstado("ACTIVO");
        usuario.setIntentosFallidos(2);
        Rol cliente = new Rol();
        cliente.setCodigo(Roles.CLIENTE);
        usuario.getRoles().add(cliente);
        return usuario;
    }
}
