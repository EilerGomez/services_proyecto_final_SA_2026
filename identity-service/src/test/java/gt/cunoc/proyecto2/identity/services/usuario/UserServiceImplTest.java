package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;
    @Mock private TokenRecuperacionRepository activaciones;
    @Mock private TokenRefrescoRepository refrescos;
    @Mock private AlmacenFotos fotos;
    @Mock private UsuarioEventPublisher publicador;

    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();
    @Spy private TokenOpacoService opacos = new TokenOpacoService();

    private UserServiceImpl servicio;

    @BeforeEach
    void preparar() {
        // Constructor manual: urlFrontend es String y Mockito no lo inyecta.
        servicio = new UserServiceImpl(usuarios, perfiles, roles, activaciones,
                refrescos, claves, opacos, fotos, publicador, "http://localhost:5173");
    }

    @Test
    void cambiarEstadoAlUltimoAdminSistemaSeRechaza() {
        // Arrange
        Usuario admin = adminSistema();
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(1L);

        // Act
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarEstado(ADMIN_ID, new UserStatusRequest("INACTIVO")));

        // Assert
        assertAll(
                () -> assertEquals("ULTIMO_ADMIN_SISTEMA", error.getCodigo()),
                () -> assertEquals("ACTIVO", admin.getEstado()),
                () -> verify(usuarios, never()).save(any()));
    }

    @Test
    void cambiarEstadoDePersonalDeSucursalSeRechaza() {
        // Arrange
        Usuario cajero = adminSistema();
        cajero.setSucursalId(UUID.randomUUID());
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(cajero));

        // Act
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarEstado(ADMIN_ID, new UserStatusRequest("INACTIVO")));

        // Assert
        assertEquals("GESTIONAR_EN_FLEET", error.getCodigo());
    }

    @Test
    void crearAdministradorSistemaPublicaCuentaConActivacion() {
        // Arrange
        var solicitud = new AdministradorSistemaRequest("nuevo@correo.com", "Ana", "López",
                "1234567-8", "1234567890101", "55551234", "Xela");
        when(roles.findAllById(Set.of("ADMIN_SISTEMA", "CLIENTE")))
                .thenReturn(List.of(rol("ADMIN_SISTEMA"), rol("CLIENTE")));

        // Act
        var respuesta = servicio.crearAdministradorSistema(solicitud);

        // Assert
        assertAll(
                () -> assertEquals("nuevo@correo.com", respuesta.correo()),
                () -> verify(publicador).publicarCuentaCreada(
                        any(), org.mockito.ArgumentMatchers.eq("nuevo@correo.com"),
                        any(), any(), org.mockito.ArgumentMatchers.startsWith("http://localhost:5173"),
                        any()),
                () -> verify(activaciones).save(any()));
    }

    @Test
    void listarPaginaMapeaFiltros() {
        // Arrange
        Usuario admin = adminSistema();
        when(usuarios.buscar(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(admin)));

        // Act
        var pagina = servicio.listar("ADMIN_SISTEMA", "ACTIVO", "admin", org.springframework.data.domain.Pageable.unpaged());

        // Assert
        assertAll(
                () -> assertEquals(1, pagina.getTotalElements()),
                () -> verify(usuarios).buscar(
                        org.mockito.ArgumentMatchers.eq("ADMIN_SISTEMA"),
                        org.mockito.ArgumentMatchers.eq("ACTIVO"),
                        org.mockito.ArgumentMatchers.eq("admin"), any()));
    }

    @Test
    void actualizarPerfilCambiaTelefonoYPublica() {
        // Arrange
        var actual = adminSistema();
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setUsuario(actual);
        perfil.setNombres("A");
        perfil.setApellidos("B");
        perfil.setNit("1");
        perfil.setDpi("1234567890101");
        perfil.setTelefono("1");
        perfil.setDireccion("X");
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));

        // Act
        var respuesta = servicio.actualizarPerfil(ADMIN_ID,
                new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                        null, null, null, "55559999", null, null));

        // Assert
        assertAll(
                () -> assertEquals("55559999", respuesta.telefono()),
                () -> verify(publicador).publicarPerfilActualizado(any(), any(), any(), any(),
                        any(), any(), any(), any(), any()));
    }

    @Test
    void actualizarPerfilConFotoNuevaBorraLaAnterior() {
        // Arrange
        String vieja = "https://cuenta.blob.core.windows.net/fotos/dev/perfiles/vieja.jpg";
        String nueva = "https://cuenta.blob.core.windows.net/fotos/dev/perfiles/nueva.jpg";
        var actual = adminSistema();
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setUsuario(actual);
        perfil.setNombres("A");
        perfil.setApellidos("B");
        perfil.setFotoUrl(vieja);
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));
        when(fotos.esDelBucket(nueva)).thenReturn(true);

        // Act
        var respuesta = servicio.actualizarPerfil(ADMIN_ID,
                new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                        null, null, null, null, null, nueva));

        // Assert
        assertAll(
                () -> assertEquals(nueva, respuesta.fotoUrl()),
                () -> verify(fotos).eliminarAlConfirmar(vieja));
    }

    @Test
    void actualizarPerfilSinCambiarFotoNoBorraNada() {
        // Arrange
        String misma = "https://cuenta.blob.core.windows.net/fotos/dev/perfiles/misma.jpg";
        var actual = adminSistema();
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setUsuario(actual);
        perfil.setNombres("A");
        perfil.setApellidos("B");
        perfil.setFotoUrl(misma);
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));
        when(fotos.esDelBucket(misma)).thenReturn(true);

        // Act
        servicio.actualizarPerfil(ADMIN_ID, new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                null, null, null, "55550000", null, misma));
        servicio.actualizarPerfil(ADMIN_ID, new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                null, null, null, "55550001", null, null));

        // Assert
        verify(fotos, never()).eliminarAlConfirmar(any());
    }

    @Test
    void actualizarPerfilNitDuplicadoDa409() {
        // Arrange
        var actual = adminSistema();
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setUsuario(actual);
        perfil.setNit("viejo");
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));
        when(perfiles.existsByNit("nuevo")).thenReturn(true);

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ConflictoException.class,
                () -> servicio.actualizarPerfil(ADMIN_ID,
                        new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                                null, null, "nuevo", null, null, null)));

        // Assert
        assertEquals("NIT_EN_USO", error.getCodigo());
    }

    @Test
    void actualizarPerfilChoferSinFotoDa422() {
        // Arrange
        var actual = adminSistema();
        actual.setChoferId(UUID.randomUUID());
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setUsuario(actual);
        perfil.setNit("1");
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ReglaNegocioException.class,
                () -> servicio.actualizarPerfil(ADMIN_ID,
                        new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                                null, null, null, null, null, null)));

        // Assert
        assertEquals("FOTO_OBLIGATORIA", error.getCodigo());
    }

    @Test
    void perfilSinPerfilDa404() {
        // Arrange
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(adminSistema()));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.empty());

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.NoEncontradoException.class,
                () -> servicio.perfil(ADMIN_ID));

        // Assert
        assertEquals("PERFIL_NO_EXISTE", error.getCodigo());
    }

    @Test
    void actualizarPerfilFotoExternaDa400() {
        // Arrange
        var actual = adminSistema();
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setUsuario(actual);
        perfil.setNit("1");
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));
        when(fotos.esDelBucket("https://otro.com/foto.jpg")).thenReturn(false);

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> servicio.actualizarPerfil(ADMIN_ID,
                        new gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest(
                                null, null, null, null, null, "https://otro.com/foto.jpg")));

        // Assert
        assertEquals("FOTO_INVALIDA", error.getCodigo());
    }

    @Test
    void contactoDeArmaNombreCompleto() {
        // Arrange
        var actual = adminSistema();
        var perfil = new gt.cunoc.proyecto2.identity.models.Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        perfil.setTelefono("5555");
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));
        when(perfiles.findById(ADMIN_ID)).thenReturn(Optional.of(perfil));

        // Act
        var respuesta = servicio.contactoDe(ADMIN_ID);

        // Assert
        assertAll(
                () -> assertEquals("Ana López", respuesta.nombre()),
                () -> assertEquals("5555", respuesta.telefono()));
    }

    @Test
    void estadoCuentaDeDevuelveEstadoYBloqueo() {
        // Arrange
        var actual = adminSistema();
        actual.setEstado("BLOQUEADO");
        actual.setBloqueadoHasta(java.time.Instant.parse("2026-10-20T15:00:00Z"));
        when(usuarios.findById(ADMIN_ID)).thenReturn(Optional.of(actual));

        // Act
        var respuesta = servicio.estadoCuentaDe(ADMIN_ID);

        // Assert
        assertAll(
                () -> assertEquals(ADMIN_ID, respuesta.usuarioId()),
                () -> assertEquals("BLOQUEADO", respuesta.estado()),
                () -> assertEquals(java.time.Instant.parse("2026-10-20T15:00:00Z"), respuesta.bloqueadoHasta()));
    }

    private Usuario adminSistema() {
        Usuario usuario = new Usuario();
        usuario.setId(ADMIN_ID);
        usuario.setCorreo("admin@correo.com");
        usuario.setEstado("ACTIVO");
        usuario.getRoles().add(rol("ADMIN_SISTEMA"));
        return usuario;
    }

    private Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }
}
