package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.CompletarPerfilRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
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

/** Completar el perfil de una cuenta creada con Google. */
@ExtendWith(MockitoExtension.class)
class CompletarPerfilTest {

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
    private Usuario usuario;
    private Perfil perfil;

    @BeforeEach
    void preparar() {
        servicio = new UserServiceImpl(usuarios, perfiles, roles, activaciones,
                refrescos, claves, opacos, fotos, publicador, "http://localhost:5173");
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("ana@gmail.com");
        perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(perfiles.findById(usuario.getId())).thenReturn(Optional.of(perfil));
    }

    private CompletarPerfilRequest datos(String nit) {
        return new CompletarPerfilRequest(nit, "1234567890101", "55551234", " Zona 1, Xela ");
    }

    @Test
    void conNitLoGuardaEnMayusculasYPublicaClienteRegistrado() {
        PerfilResponse respuesta = servicio.completarPerfil(usuario.getId(), datos(" 1234567-k "));

        assertThat(perfil.estaCompleto()).isTrue();
        assertThat(perfil.getNit()).isEqualTo("1234567-K");
        assertThat(perfil.getDireccion()).isEqualTo("Zona 1, Xela");
        assertThat(respuesta.dpi()).isEqualTo("1234567890101");
        verify(perfiles).save(perfil);
        verify(publicador).publicarClienteRegistrado(usuario.getId(), "1234567-K", "Ana López", "ana@gmail.com",
                LocalDate.now());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"cf", " CF "})
    void sinNitQuedaNullYSalesLoTrataComoCf(String nit) {
        servicio.completarPerfil(usuario.getId(), datos(nit));

        assertThat(perfil.getNit()).isNull();
        verify(perfiles, never()).existsByNit(any());
        verify(publicador).publicarClienteRegistrado(usuario.getId(), null, "Ana López", "ana@gmail.com",
                LocalDate.now());
    }

    @Test
    void unPerfilCompletoNoSeVuelveACompletar() {
        perfil.setDpi("9999999999999");
        perfil.setTelefono("55550000");
        perfil.setDireccion("Xela");

        assertThatThrownBy(() -> servicio.completarPerfil(usuario.getId(), datos(null)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERFIL_YA_COMPLETO"));
        verifyNoInteractions(publicador);
    }

    @Test
    void dpiRepetido() {
        when(perfiles.existsByDpi("1234567890101")).thenReturn(true);

        assertThatThrownBy(() -> servicio.completarPerfil(usuario.getId(), datos(null)))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("DPI_EN_USO"));
        verify(perfiles, never()).save(any());
    }

    @Test
    void nitRepetido() {
        when(perfiles.existsByNit("1234567-8")).thenReturn(true);

        assertThatThrownBy(() -> servicio.completarPerfil(usuario.getId(), datos("1234567-8")))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("NIT_EN_USO"));
        verifyNoInteractions(publicador);
    }
}
