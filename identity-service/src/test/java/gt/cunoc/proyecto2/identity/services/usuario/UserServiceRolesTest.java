package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioBusquedaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Búsqueda por correo y promoción a administrador del sistema. */
@ExtendWith(MockitoExtension.class)
class UserServiceRolesTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;
    @Mock private TokenRecuperacionRepository activaciones;
    @Mock private TokenRefrescoRepository refrescos;
    @Mock private PasswordEncoder claves;
    @Mock private TokenOpacoService opacos;
    @Mock private AlmacenFotos fotos;
    @Mock private UsuarioEventPublisher publicador;

    private UserServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new UserServiceImpl(usuarios, perfiles, roles, activaciones,
                refrescos, claves, opacos, fotos, publicador, "http://localhost:5173");
    }

    @Test
    void buscarPorCorreoNormalizaYDiceSiPuedeSerPersonal() {
        // Arrange
        Usuario cliente = usuario(Roles.CLIENTE);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(cliente));
        Perfil perfil = perfil();
        perfil.setFotoUrl("https://foto");
        when(perfiles.findById(cliente.getId())).thenReturn(Optional.of(perfil));

        // Act
        UsuarioBusquedaResponse respuesta = servicio.buscarPorCorreo("  Ana@Correo.com ", false);

        // Assert
        assertThat(respuesta).isEqualTo(new UsuarioBusquedaResponse(cliente.getId(), "ana@correo.com",
                "Ana López", "https://foto", true, null));
    }

    @Test
    void buscarPorCorreoDevuelveElMotivoSiNoPuedeSerPersonal() {
        // Arrange
        Usuario chofer = usuario(Roles.CLIENTE, Roles.CHOFER);
        chofer.setChoferId(UUID.randomUUID());
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(chofer));
        when(perfiles.findById(chofer.getId())).thenReturn(Optional.empty());

        // Act
        UsuarioBusquedaResponse respuesta = servicio.buscarPorCorreo("ana@correo.com", true);

        // Assert
        assertThat(respuesta.puedeSerPersonal()).isFalse();
        assertThat(respuesta.motivo()).contains("chofer");
        assertThat(respuesta.nombreCompleto()).isEqualTo("ana@correo.com");
        assertThat(respuesta.fotoUrl()).isNull();
    }

    @Test
    void alAdminDeSucursalNoSeLeDiceSiEsAdminDelSistema() {
        // Arrange
        Usuario admin = usuario(Roles.ADMIN_SISTEMA, Roles.CLIENTE);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(admin));
        when(perfiles.findById(admin.getId())).thenReturn(Optional.empty());

        // Act
        UsuarioBusquedaResponse respuesta = servicio.buscarPorCorreo("ana@correo.com", false);

        // Assert
        assertThat(respuesta.puedeSerPersonal()).isFalse();
        assertThat(respuesta.motivo()).isEqualTo("Esta cuenta no se puede asignar como personal de sucursal");
    }

    @Test
    void alAdminDeSucursalSiSeLeDiceQueYaEsPersonal() {
        // Arrange
        Usuario cajero = usuario(Roles.CAJERO, Roles.CLIENTE);
        cajero.setSucursalId(UUID.randomUUID());
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(cajero));
        when(perfiles.findById(cajero.getId())).thenReturn(Optional.empty());

        // Act + Assert
        assertThat(servicio.buscarPorCorreo("ana@correo.com", false).motivo())
                .isEqualTo("La persona ya trabaja en una sucursal");
    }

    @Test
    void estadoDeCuentaIndicaSiNoPuedeSerPersonal() {
        // Arrange
        Usuario chofer = usuario(Roles.CLIENTE);
        chofer.setChoferId(UUID.randomUUID());
        Usuario cliente = usuario(Roles.CLIENTE);
        when(usuarios.findById(chofer.getId())).thenReturn(Optional.of(chofer));
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        // Act + Assert
        assertThat(servicio.estadoCuentaDe(chofer.getId()).impedimentoPersonal()).contains("chofer");
        assertThat(servicio.estadoCuentaDe(cliente.getId()).impedimentoPersonal()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void buscarSinCorreoDa400(String correo) {
        assertThatThrownBy(() -> servicio.buscarPorCorreo(correo, true))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CORREO_REQUERIDO"));
        verifyNoInteractions(usuarios);
    }

    @Test
    void buscarCorreoInexistenteDa404() {
        when(usuarios.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorCorreo("nadie@correo.com", true))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("USUARIO_NO_EXISTE"));
    }

    @Test
    void hacerAdministradorSistemaAgregaElRolYAvisa() {
        // Arrange
        Usuario cliente = usuario(Roles.CLIENTE);
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(roles.findById(Roles.ADMIN_SISTEMA)).thenReturn(Optional.of(rol(Roles.ADMIN_SISTEMA)));
        when(perfiles.findById(cliente.getId())).thenReturn(Optional.of(perfil()));

        // Act
        UsuarioDetalleResponse detalle = servicio.hacerAdministradorSistema(cliente.getId());

        // Assert
        assertThat(detalle.roles()).containsExactly(Roles.ADMIN_SISTEMA, Roles.CLIENTE);
        verify(usuarios).save(cliente);
        verify(publicador).publicarRolAsignado(cliente.getId(), "ana@correo.com", "Ana López",
                Roles.ADMIN_SISTEMA, null, null, LocalDate.now());
    }

    @Test
    void hacerAdministradorSistemaAUnCajeroSeRechaza() {
        // Arrange
        Usuario cajero = usuario(Roles.CLIENTE, Roles.CAJERO);
        cajero.setSucursalId(UUID.randomUUID());
        when(usuarios.findById(cajero.getId())).thenReturn(Optional.of(cajero));

        // Act + Assert
        assertThatThrownBy(() -> servicio.hacerAdministradorSistema(cajero.getId()))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ES_PERSONAL_SUCURSAL"));
        verify(usuarios, never()).save(any());
        verifyNoInteractions(publicador);
    }

    @Test
    void hacerAdministradorSistemaSinRolEnCatalogoDa422() {
        // Arrange
        Usuario cliente = usuario(Roles.CLIENTE);
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(roles.findById(Roles.ADMIN_SISTEMA)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> servicio.hacerAdministradorSistema(cliente.getId()))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ROL_FALTANTE"));
    }

    private static Usuario usuario(String... codigos) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("ana@correo.com");
        usuario.setEstado("ACTIVO");
        for (String codigo : codigos) {
            usuario.getRoles().add(rol(codigo));
        }
        return usuario;
    }

    private static Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }

    private static Perfil perfil() {
        Perfil perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        return perfil;
    }
}
