package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.DobleFactorRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
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
import gt.cunoc.proyecto2.identity.services.oauth.CodigosUnSoloUso;
import gt.cunoc.proyecto2.identity.services.oauth.GoogleOAuthProperties;
import gt.cunoc.proyecto2.identity.services.oauth.VerificadorGoogle;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas borde de AuthServiceImpl no cubiertas por AuthServiceImplTest. */
@ExtendWith(MockitoExtension.class)
class AuthServiceRamasTest {

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

    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();
    @Spy private TokenOpacoService opacos = new TokenOpacoService();
    @Mock private VigilanciaAcceso vigilancia;
    @Mock private VerificadorGoogle google;

    private AuthServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthServiceImpl(usuarios, perfiles, roles, carteras, refrescos,
                recuperaciones, desafios, claves, jwt, 60L, new IdentidadUsuarioFactory(),
                opacos, vigilancia, publicador, servicioUsuarios, "http://localhost:5173",
                new GoogleOAuthProperties(), new CodigosUnSoloUso(opacos), google);
    }

    private Usuario usuarioActivo() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("a@correo.com");
        usuario.setClaveCifrada(claves.encode("Secreta123"));
        usuario.setEstado("ACTIVO");
        Rol rol = new Rol();
        rol.setCodigo("CLIENTE");
        usuario.setRoles(Set.of(rol));
        return usuario;
    }

    private RegistroRequest registro() {
        return new RegistroRequest("n@correo.com", "Secreta123", "Ana", "López", "CF",
                "1234567890123", "12345678", "Ciudad", null);
    }

    @Test
    void registroNitDuplicadoDa409() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrar(registro()));
        assertEquals("NIT_EN_USO", error.getCodigo());
    }

    @Test
    void registroDpiDuplicadoDa409() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrar(registro()));
        assertEquals("DPI_EN_USO", error.getCodigo());
    }

    @Test
    void registroSinRolClienteDa422() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(false);
        when(roles.findById("CLIENTE")).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(registro()));
        assertEquals("ROL_FALTANTE", error.getCodigo());
    }

    @Test
    void loginUsuarioInexistenteNoRegistraFallo() {
        when(usuarios.findByCorreo("x@correo.com")).thenReturn(Optional.empty());

        assertThrows(NoAutenticadoException.class,
                () -> servicio.iniciarSesion(new InicioSesionRequest("x@correo.com", "y")));
        verify(vigilancia, never()).falloContrasena(any());
    }

    @Test
    void loginBloqueadoVigenteDa401() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado("BLOQUEADO");
        usuario.setBloqueadoHasta(Instant.now().plusSeconds(3600));
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));

        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.iniciarSesion(new InicioSesionRequest("a@correo.com", "Secreta123")));
        assertEquals("CUENTA_BLOQUEADA", error.getCodigo());
    }

    @Test
    void loginDobleFactorSinPerfilUsaCorreo() {
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var resultado = servicio.iniciarSesion(new InicioSesionRequest("a@correo.com", "Secreta123"));

        assertTrue(resultado instanceof gt.cunoc.proyecto2.identity.dtos.auth.DesafioDobleFactor);
        verify(desafios).save(any(CodigoDobleFactor.class));
    }

    @Test
    void verificarDobleFactorSinUsuarioOInactivoOSinFlagDa401() {
        when(usuarios.findByCorreo("x@correo.com")).thenReturn(Optional.empty());
        assertThrows(NoAutenticadoException.class, () -> servicio.verificarDobleFactor(
                new VerificarDobleFactorRequest("x@correo.com", "123456")));

        Usuario inactivo = usuarioActivo();
        inactivo.setEstado("INACTIVO");
        when(usuarios.findByCorreo("i@correo.com")).thenReturn(Optional.of(inactivo));
        assertThrows(NoAutenticadoException.class, () -> servicio.verificarDobleFactor(
                new VerificarDobleFactorRequest("i@correo.com", "123456")));

        Usuario sinFlag = usuarioActivo();
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(sinFlag));
        assertThrows(NoAutenticadoException.class, () -> servicio.verificarDobleFactor(
                new VerificarDobleFactorRequest("a@correo.com", "123456")));
    }

    @Test
    void verificarDobleFactorSinDesafioDa401() {
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(desafios.findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(usuario.getId()))
                .thenReturn(List.of());

        assertThrows(NoAutenticadoException.class, () -> servicio.verificarDobleFactor(
                new VerificarDobleFactorRequest("a@correo.com", "123456")));
    }

    @Test
    void verificarDobleFactorVencidoDa401() {
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        CodigoDobleFactor desafio = new CodigoDobleFactor();
        desafio.setExpiraEn(Instant.now().minusSeconds(60));
        when(desafios.findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(usuario.getId()))
                .thenReturn(List.of(desafio));

        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.verificarDobleFactor(
                        new VerificarDobleFactorRequest("a@correo.com", "123456")));
        assertEquals("DOBLE_FACTOR_VENCIDO", error.getCodigo());
    }

    @Test
    void verificarDobleFactorHashDistintoRegistraFallo() {
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        CodigoDobleFactor desafio = new CodigoDobleFactor();
        desafio.setId(UUID.randomUUID());
        desafio.setCodigoHash("otro-hash");
        desafio.setExpiraEn(Instant.now().plusSeconds(600));
        when(desafios.findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(usuario.getId()))
                .thenReturn(List.of(desafio));

        assertThrows(NoAutenticadoException.class, () -> servicio.verificarDobleFactor(
                new VerificarDobleFactorRequest("a@correo.com", "123456")));
        verify(vigilancia).falloCodigo(desafio.getId(), usuario.getId());
    }

    @Test
    void renovarExpiradoDa401() {
        TokenRefresco guardado = new TokenRefresco();
        guardado.setExpiraEn(Instant.now().minusSeconds(60));
        when(refrescos.findByTokenHash(any())).thenReturn(Optional.of(guardado));

        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.renovarSesion(
                        new gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest("t")));
        assertEquals("TOKEN_REFRESCO_INVALIDO", error.getCodigo());
    }

    @Test
    void renovarUsuarioInexistenteDa401() {
        TokenRefresco guardado = new TokenRefresco();
        guardado.setUsuarioId(UUID.randomUUID());
        guardado.setExpiraEn(Instant.now().plusSeconds(600));
        when(refrescos.findByTokenHash(any())).thenReturn(Optional.of(guardado));
        when(usuarios.findById(guardado.getUsuarioId())).thenReturn(Optional.empty());

        assertThrows(NoAutenticadoException.class, () -> servicio.renovarSesion(
                new gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest("t")));
    }

    @Test
    void renovarCuentaInactivaDa401() {
        TokenRefresco guardado = new TokenRefresco();
        guardado.setUsuarioId(UUID.randomUUID());
        guardado.setExpiraEn(Instant.now().plusSeconds(600));
        when(refrescos.findByTokenHash(any())).thenReturn(Optional.of(guardado));
        Usuario usuario = usuarioActivo();
        usuario.setEstado("INACTIVO");
        when(usuarios.findById(guardado.getUsuarioId())).thenReturn(Optional.of(usuario));

        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.renovarSesion(
                        new gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest("t")));
        assertEquals("CUENTA_INACTIVA", error.getCodigo());
    }

    @Test
    void cerrarSesionInexistenteYDeOtroNoRevoca() {
        UUID mio = UUID.randomUUID();
        when(refrescos.findByTokenHash(any())).thenReturn(Optional.empty());
        servicio.cerrarSesion(mio, new gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest("t"));

        TokenRefresco ajeno = new TokenRefresco();
        ajeno.setUsuarioId(UUID.randomUUID());
        when(refrescos.findByTokenHash(any())).thenReturn(Optional.of(ajeno));
        servicio.cerrarSesion(mio, new gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest("t"));

        TokenRefresco revocado = new TokenRefresco();
        revocado.setUsuarioId(mio);
        revocado.setRevocadoEn(Instant.now());
        when(refrescos.findByTokenHash(any())).thenReturn(Optional.of(revocado));
        servicio.cerrarSesion(mio, new gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest("t"));

        verify(refrescos, never()).save(any(TokenRefresco.class));
    }

    @Test
    void recuperarInactivoNoPublica() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado("INACTIVO");
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));

        servicio.solicitarRecuperacion(
                new RecuperacionRequest("a@correo.com"));

        verify(publicador, never()).publicarRecuperacionContrasenaSolicitada(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void restablecerTokenInexistenteDa404() {
        when(recuperaciones.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.restablecerContrasena(
                new RestablecimientoRequest("t", "Nueva12345")));
    }

    @Test
    void restablecerExpiradoDa422() {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setExpiraEn(Instant.now().minusSeconds(60));
        when(recuperaciones.findByTokenHash(any())).thenReturn(Optional.of(token));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.restablecerContrasena(
                        new RestablecimientoRequest("t", "Nueva12345")));
        assertEquals("TOKEN_VENCIDO", error.getCodigo());
    }

    @Test
    void restablecerUsuarioInexistenteDa404() {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuarioId(UUID.randomUUID());
        token.setExpiraEn(Instant.now().plusSeconds(600));
        when(recuperaciones.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(usuarios.findById(token.getUsuarioId())).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.restablecerContrasena(
                new RestablecimientoRequest("t", "Nueva12345")));
    }

    @Test
    void restablecerDesbloqueaCuentaBloqueada() {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuarioId(UUID.randomUUID());
        token.setExpiraEn(Instant.now().plusSeconds(600));
        when(recuperaciones.findByTokenHash(any())).thenReturn(Optional.of(token));
        Usuario usuario = usuarioActivo();
        usuario.setEstado("BLOQUEADO");
        when(usuarios.findById(token.getUsuarioId())).thenReturn(Optional.of(usuario));
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId())).thenReturn(List.of());

        servicio.restablecerContrasena(new RestablecimientoRequest("t", "Nueva12345"));

        assertEquals("ACTIVO", usuario.getEstado());
        assertEquals(null, usuario.getBloqueadoHasta());
    }

    @Test
    void cambiarDobleFactorInexistenteDa404() {
        UUID id = UUID.randomUUID();
        when(usuarios.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.cambiarDobleFactor(id, new DobleFactorRequest(true)));
    }

    @Test
    void cambiarDobleFactorAceptaNuloComoApagado() {
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        servicio.cambiarDobleFactor(usuario.getId(), new DobleFactorRequest(null));

        assertEquals(false, usuario.isDobleFactorActivo());
    }

    @Test
    void crearTokensSinPerfilUsaCorreo() {
        Usuario usuario = usuarioActivo();
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());
        when(jwt.generar(any())).thenReturn("jwt");

        var sesion = servicio.iniciarSesion(new InicioSesionRequest("a@correo.com", "Secreta123"));

        assertTrue(sesion instanceof gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse);
    }
}
