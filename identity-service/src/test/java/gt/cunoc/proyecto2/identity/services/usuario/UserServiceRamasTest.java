package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRefresco;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas borde de UserServiceImpl: duplicados, nulos, filtros y estados. */
@ExtendWith(MockitoExtension.class)
class UserServiceRamasTest {

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
        return perfil;
    }

    private AdministradorSistemaRequest solicitudAdmin() {
        return new AdministradorSistemaRequest("n@correo.com", "Ana", "López", "CF",
                "1234567890123", "12345678", "Ciudad");
    }

    @Test
    void crearCorreoDuplicadoDa409() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crearAdministradorSistema(solicitudAdmin()));
        assertEquals("CORREO_EN_USO", error.getCodigo());
    }

    @Test
    void crearNitDuplicadoDa409() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crearAdministradorSistema(solicitudAdmin()));
        assertEquals("NIT_EN_USO", error.getCodigo());
    }

    @Test
    void crearDpiDuplicadoDa409() {
        when(usuarios.existsByCorreo("n@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crearAdministradorSistema(solicitudAdmin()));
        assertEquals("DPI_EN_USO", error.getCodigo());
    }

    @Test
    void listarSinPerfilUsaCorreoYNitNulo() {
        Usuario usuario = usuarioBasico();
        when(usuarios.buscar(any(), any(), any(), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(usuario)));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var pagina = servicio.listar("", "  ", null, Pageable.unpaged());

        assertAll(
                () -> assertEquals(1, pagina.getTotalElements()),
                () -> assertEquals("a@correo.com", pagina.getContent().get(0).nombre()),
                () -> assertEquals(null, pagina.getContent().get(0).nit()));
    }

    @Test
    void detalleSinPerfilMapeaNulos() {
        Usuario usuario = usuarioBasico();
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var detalle = servicio.detalle(usuario.getId());

        assertEquals(null, detalle.nombres());
    }

    @Test
    void contactoSinPerfilUsaCorreo() {
        Usuario usuario = usuarioBasico();
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        var contacto = servicio.contactoDe(usuario.getId());

        assertAll(
                () -> assertEquals("a@correo.com", contacto.nombre()),
                () -> assertEquals(null, contacto.telefono()));
    }

    @Test
    void administradoresFiltraInactivosYSinPerfil() {
        UUID sucursalId = UUID.randomUUID();
        Usuario activo = usuarioBasico();
        Usuario inactivo = usuarioBasico();
        inactivo.setEstado("INACTIVO");
        when(usuarios.findBySucursalIdAndRoles_Codigo(sucursalId, "ADMIN_SUCURSAL"))
                .thenReturn(List.of(activo, inactivo));
        when(perfiles.findById(activo.getId())).thenReturn(Optional.empty());

        var lista = servicio.administradoresDe(sucursalId);

        assertAll(
                () -> assertEquals(1, lista.size()),
                () -> assertEquals("a@correo.com", lista.get(0).nombre()));
    }

    @Test
    void actualizarTodoNuloGuardaIgual() {
        Usuario usuario = usuarioBasico();
        Perfil perfil = perfilBasico();
        perfil.setNit("CF");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));

        var respuesta = servicio.actualizarPerfil(usuario.getId(),
                new UpdateProfileRequest(null, null, null, null, null, null));

        assertEquals("Ana", respuesta.nombres());
        verify(perfiles).save(perfil);
    }

    @Test
    void actualizarNitIgualNoRevisaDuplicado() {
        Usuario usuario = usuarioBasico();
        Perfil perfil = perfilBasico();
        perfil.setNit("CF");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));

        servicio.actualizarPerfil(usuario.getId(),
                new UpdateProfileRequest(null, null, "CF", null, null, null));

        verify(perfiles, org.mockito.Mockito.never()).existsByNit(any());
        assertEquals(null, perfil.getNit());      // CF se guarda sin NIT
    }

    @Test
    void nitVacioQuitaElNitSinRevisarDuplicados() {
        Usuario usuario = usuarioBasico();
        Perfil perfil = perfilBasico();
        perfil.setNit("1234567-8");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));

        servicio.actualizarPerfil(usuario.getId(), new UpdateProfileRequest(null, null, "  ", null, null, null));

        assertEquals(null, perfil.getNit());
        verify(perfiles, org.mockito.Mockito.never()).existsByNit(any());
        verify(perfiles).save(perfil);
    }

    @Test
    void actualizarFotoValidaYChoferConFoto() {
        Usuario usuario = usuarioBasico();
        usuario.setChoferId(UUID.randomUUID());
        Perfil perfil = perfilBasico();
        perfil.setFotoUrl("https://foto.jpg");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));
        when(fotos.esDelBucket("https://foto.jpg")).thenReturn(true);

        var respuesta = servicio.actualizarPerfil(usuario.getId(),
                new UpdateProfileRequest(null, null, null, null, null, "https://foto.jpg"));

        assertEquals("https://foto.jpg", respuesta.fotoUrl());
    }

    @Test
    void cambiarEstadoOkRevocaTokens() {
        Usuario usuario = usuarioBasico();
        Rol rol = new Rol();
        rol.setCodigo("CLIENTE");
        usuario.setRoles(Set.of(rol));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        TokenRefresco token = new TokenRefresco();
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()))
                .thenReturn(List.of(token));

        var detalle = servicio.cambiarEstado(usuario.getId(), new UserStatusRequest("INACTIVO"));

        assertAll(
                () -> assertEquals("INACTIVO", detalle.estado()),
                () -> assertTrue(token.getRevocadoEn() != null),
                () -> verify(refrescos).save(token),
                // Sus JWT dejan de valer al instante: el API Gateway recibe SesionRevocada
                () -> assertTrue(usuario.getTokensValidosDesde() != null),
                () -> verify(publicador).publicarSesionRevocada(
                        eq(usuario.getId()), eq(usuario.getTokensValidosDesde()), any()));
    }

    @Test
    void activarUnaCuentaNoRevocaSusTokens() {
        Usuario usuario = usuarioBasico();
        usuario.setEstado("BLOQUEADO");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        servicio.cambiarEstado(usuario.getId(), new UserStatusRequest("ACTIVO"));

        assertEquals(null, usuario.getTokensValidosDesde());
        verify(publicador, org.mockito.Mockito.never()).publicarSesionRevocada(any(), any(), any());
    }

    @Test
    void cambiarEstadoUsuarioYaInactivoNoCuentaAdmins() {
        Usuario usuario = usuarioBasico();
        usuario.setEstado("INACTIVO");
        Rol rol = new Rol();
        rol.setCodigo("ADMIN_SISTEMA");
        usuario.setRoles(Set.of(rol));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId())).thenReturn(List.of());
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.empty());

        usuario.setBloqueadoHasta(java.time.Instant.now().plusSeconds(600));
        usuario.setIntentosFallidos(3);

        var detalle = servicio.cambiarEstado(usuario.getId(), new UserStatusRequest("BLOQUEADO"));

        assertEquals("BLOQUEADO", detalle.estado());
        // Bloqueo del administrador: sin fecha, no vence solo
        assertEquals(null, usuario.getBloqueadoHasta());
        assertEquals(0, usuario.getIntentosFallidos());
        verify(usuarios, org.mockito.Mockito.never())
                .countByEstadoAndRoles_Codigo(any(), any());
    }

    @Test
    void perfilUsuarioInexistenteDa404() {
        UUID id = UUID.randomUUID();
        when(usuarios.findById(id)).thenReturn(Optional.empty());

        assertThrows(gt.cunoc.proyecto2.comun.errores.NoEncontradoException.class,
                () -> servicio.perfil(id));
    }
}
