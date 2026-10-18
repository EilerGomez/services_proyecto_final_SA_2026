package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas de UserServiceImpl sin cubrir: perfil presente, campos de edición, chofer y último admin. */
@ExtendWith(MockitoExtension.class)
class UserServiceCoberturaTest {

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
        servicio = new UserServiceImpl(usuarios, perfiles, roles, activaciones,
                refrescos, claves, opacos, fotos, publicador, "http://localhost:5173");
    }

    private Usuario usuarioBasico() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("a@correo.com");
        usuario.setEstado("ACTIVO");
        return usuario;
    }

    private Perfil perfilBasico() {
        Perfil perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        perfil.setNit("CF");
        perfil.setDpi("1234567890123");
        perfil.setTelefono("5555");
        perfil.setDireccion("Xela");
        return perfil;
    }

    private Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }

    @Test
    void listarConPerfilMapeaNombreYNit() {
        Usuario usuario = usuarioBasico();
        when(usuarios.buscar(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario)));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfilBasico()));

        var pagina = servicio.listar("CLIENTE", "ACTIVO", "ana", Pageable.unpaged());

        assertAll(
                () -> assertEquals(1, pagina.getTotalElements()),
                () -> assertEquals("Ana López", pagina.getContent().get(0).nombre()),
                () -> assertEquals("CF", pagina.getContent().get(0).nit()));
    }

    @Test
    void actualizarPerfilCompletoAplicaCambios() {
        Usuario usuario = usuarioBasico();
        Perfil perfil = perfilBasico();
        perfil.setNit("viejo");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));
        when(perfiles.existsByNit("nuevo")).thenReturn(false);
        when(fotos.esDelBucket("https://stproyecto2fotos.blob.core.windows.net/fotos/perfiles/foto.jpg")).thenReturn(true);

        var respuesta = servicio.actualizarPerfil(usuario.getId(), new UpdateProfileRequest(
                "Nuevo", "Pérez", "nuevo", null, "Nueva dirección",
                "https://stproyecto2fotos.blob.core.windows.net/fotos/perfiles/foto.jpg"));

        assertAll(
                () -> assertEquals("Nuevo", respuesta.nombres()),
                () -> assertEquals("Pérez", respuesta.apellidos()),
                () -> assertEquals("nuevo", respuesta.nit()),
                () -> assertEquals("Nueva dirección", respuesta.direccion()),
                () -> verify(perfiles).save(perfil));
    }

    @Test
    void actualizarPerfilSinPerfilDa404() {
        Usuario usuario = usuarioBasico();
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        NoEncontradoException error = assertThrows(NoEncontradoException.class,
                () -> servicio.actualizarPerfil(usuario.getId(),
                        new UpdateProfileRequest(null, null, null, null, null, null)));

        assertEquals("PERFIL_NO_EXISTE", error.getCodigo());
    }

    @Test
    void actualizarPerfilChoferFotoEnBlancoDa422() {
        Usuario usuario = usuarioBasico();
        usuario.setChoferId(UUID.randomUUID());
        Perfil perfil = perfilBasico();
        perfil.setFotoUrl("   ");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarPerfil(usuario.getId(),
                        new UpdateProfileRequest(null, null, null, null, null, null)));

        assertEquals("FOTO_OBLIGATORIA", error.getCodigo());
    }

    @Test
    void perfilConPerfilDevuelveRespuesta() {
        Usuario usuario = usuarioBasico();
        usuario.getRoles().add(rol("CLIENTE"));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfilBasico()));

        var respuesta = servicio.perfil(usuario.getId());

        assertAll(
                () -> assertEquals("Ana", respuesta.nombres()),
                () -> assertEquals("CF", respuesta.nit()));
    }

    @Test
    void cambiarEstadoMismoEstadoNoCuentaAdmins() {
        Usuario usuario = usuarioBasico();
        usuario.getRoles().add(rol("ADMIN_SISTEMA"));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId())).thenReturn(List.of());
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var detalle = servicio.cambiarEstado(usuario.getId(), new UserStatusRequest("ACTIVO"));

        assertAll(
                () -> assertEquals("ACTIVO", detalle.estado()),
                () -> verify(usuarios, never())
                        .countByEstadoAndRoles_Codigo(any(), any()));
    }

    @Test
    void cambiarEstadoChoferSeRechaza() {
        Usuario usuario = usuarioBasico();
        usuario.setChoferId(UUID.randomUUID());
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarEstado(usuario.getId(), new UserStatusRequest("INACTIVO")));

        assertEquals("GESTIONAR_EN_FLEET", error.getCodigo());
    }

    @Test
    void cambiarEstadoAdminConOtroActivoProcede() {
        Usuario usuario = usuarioBasico();
        usuario.getRoles().add(rol("ADMIN_SISTEMA"));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(2L);
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId())).thenReturn(List.of());
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var detalle = servicio.cambiarEstado(usuario.getId(), new UserStatusRequest("INACTIVO"));

        assertAll(
                () -> assertEquals("INACTIVO", detalle.estado()),
                () -> verify(usuarios).save(usuario));
    }

    @Test
    void administradoresConPerfilMapeaNombre() {
        UUID sucursalId = UUID.randomUUID();
        Usuario activo = usuarioBasico();
        when(usuarios.findBySucursalIdAndRoles_Codigo(sucursalId, "ADMIN_SUCURSAL"))
                .thenReturn(List.of(activo));
        when(perfiles.findById(activo.getId())).thenReturn(Optional.of(perfilBasico()));

        var lista = servicio.administradoresDe(sucursalId);

        assertAll(
                () -> assertEquals(1, lista.size()),
                () -> assertEquals("Ana López", lista.get(0).nombre()),
                () -> verify(usuarios, never()).save(any()));
    }
}
