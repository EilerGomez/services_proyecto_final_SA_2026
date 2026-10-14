package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas de AuthServiceImpl sin cubrir: registro exitoso, desbloqueo, nombres desde perfil y 404. */
@ExtendWith(MockitoExtension.class)
class AuthServiceCoberturaTest {

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

    private AuthServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthServiceImpl(usuarios, perfiles, roles, carteras, refrescos,
                recuperaciones, desafios, claves, jwt, 60L, new IdentidadUsuarioFactory(),
                opacos, vigilancia, publicador, servicioUsuarios, "http://localhost:5173");
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

    private Perfil perfilAna() {
        Perfil perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        perfil.setNit("CF");
        perfil.setDpi("1234567890123");
        perfil.setTelefono("5555");
        perfil.setDireccion("Xela");
        perfil.setFotoUrl("https://fotos/foto.jpg");
        return perfil;
    }

    private RegistroRequest registro() {
        return new RegistroRequest("n@correo.com", "Secreta123", "Ana", "López", "CF",
                "1234567890123", "12345678", "Ciudad", null);
    }

    @Test
    void registroExitosoPublicaEventos() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(false);
        Rol cliente = new Rol();
        cliente.setCodigo("CLIENTE");
        when(roles.findById("CLIENTE")).thenReturn(Optional.of(cliente));

        var respuesta = servicio.registrar(registro());

        assertAll(
                () -> assertEquals("n@correo.com", respuesta.correo()),
                () -> verify(carteras).save(any()),
                () -> verify(publicador).publicarCuentaCreada(
                        any(), eq("n@correo.com"), eq("Ana López"), any(), any(), any()),
                () -> verify(publicador).publicarClienteRegistrado(
                        any(), eq("CF"), eq("Ana López"), eq("n@correo.com"), any()));
    }

    @Test
    void loginBloqueadoSinFechaDesbloquea() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado("BLOQUEADO");
        usuario.setBloqueadoHasta(null);
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt");
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var resultadoBloqueo = servicio.iniciarSesion(new InicioSesionRequest("a@correo.com", "Secreta123"));
        var respuesta = (gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse) resultadoBloqueo;

        assertAll(
                () -> assertEquals("ACTIVO", usuario.getEstado()),
                () -> assertEquals("jwt", respuesta.tokenAcceso()),
                () -> assertEquals("a@correo.com", respuesta.usuario().nombre()));
    }

    @Test
    void solicitarRecuperacionConPerfilUsaNombre() {
        Usuario usuario = usuarioActivo();
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfilAna()));

        servicio.solicitarRecuperacion(new RecuperacionRequest("a@correo.com"));

        verify(publicador).publicarRecuperacionContrasenaSolicitada(
                eq(usuario.getId()), eq("a@correo.com"), eq("Ana López"), any(), any(), any());
    }

    @Test
    void cambiarContrasenaSinUsuarioDa404() {
        UUID id = UUID.randomUUID();
        when(usuarios.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.cambiarContrasena(id,
                        new CambioContrasenaRequest("Secreta123", "Nueva#2026")));
    }

    @Test
    void loginDobleFactorConPerfilUsaNombre() {
        Usuario usuario = usuarioActivo();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfilAna()));

        var resultado = servicio.iniciarSesion(new InicioSesionRequest("a@correo.com", "Secreta123"));

        assertEquals("a@correo.com", ((gt.cunoc.proyecto2.identity.dtos.auth.DesafioDobleFactor) resultado).correo());
        verify(publicador).publicarCodigoDobleFactor(
                eq(usuario.getId()), eq("a@correo.com"), eq("Ana López"), any(), any(), any());
    }

    @Test
    void loginConPerfilMapeaNombreYFoto() {
        Usuario usuario = usuarioActivo();
        when(usuarios.findByCorreo("a@correo.com")).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt");
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfilAna()));

        var resultadoSesion = servicio.iniciarSesion(new InicioSesionRequest("a@correo.com", "Secreta123"));
        var sesion = (gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse) resultadoSesion;

        assertAll(
                () -> assertEquals("Ana López", sesion.usuario().nombre()),
                () -> assertEquals("https://fotos/foto.jpg", sesion.usuario().fotoUrl()));
    }
}
