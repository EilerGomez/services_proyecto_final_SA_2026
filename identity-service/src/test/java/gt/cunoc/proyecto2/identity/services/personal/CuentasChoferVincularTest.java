package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.VincularCuentaRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.SolicitudCuentaRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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

/** R5b para CHOFER: validar una cuenta existente (el rol llega con ChoferCreado) y su foto. */
@ExtendWith(MockitoExtension.class)
class CuentasChoferVincularTest {

    private static final UUID CHOFER_ID = UUID.randomUUID();
    private static final UUID SUCURSAL_ID = UUID.randomUUID();
    private static final String FOTO = "https://fotos/dev/choferes/nueva.jpg";

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;
    @Mock private SolicitudCuentaRepository solicitudes;
    @Mock private TokenRecuperacionRepository activaciones;
    @Mock private PasswordEncoder claves;
    @Mock private TokenOpacoService opacos;
    @Mock private AlmacenFotos fotos;
    @Mock private UsuarioEventPublisher publicador;

    private CuentasPersonalServiceImpl servicio;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        servicio = new CuentasPersonalServiceImpl(usuarios, perfiles, roles, solicitudes, activaciones,
                claves, opacos, fotos, publicador, "http://localhost:5173");
        cliente = ReglasAsignacionRolTest.usuario("ACTIVO", Roles.CLIENTE);
    }

    @Test
    void conFotoNuevaLaGuardaEnElPerfilYRegistraLaSolicitudSinDarElRol() {
        Perfil perfil = perfil(null);
        cuentaNueva(perfil);
        when(fotos.esDelBucket(FOTO)).thenReturn(true);

        servicio.vincular(solicitud(FOTO));

        assertThat(perfil.getFotoUrl()).isEqualTo(FOTO);
        verify(perfiles).save(perfil);
        ArgumentCaptor<SolicitudCuentaPersonal> registro = ArgumentCaptor.forClass(SolicitudCuentaPersonal.class);
        verify(solicitudes).save(registro.capture());
        assertThat(registro.getValue().getSolicitudId()).isEqualTo(CHOFER_ID);
        assertThat(registro.getValue().getRolCodigo()).isEqualTo(Roles.CHOFER);
        assertThat(cliente.getChoferId()).isNull();
        assertThat(cliente.getRoles()).extracting(Rol::getCodigo).containsExactly(Roles.CLIENTE);
        verifyNoInteractions(publicador, roles);
    }

    @Test
    void sinFotoNuevaUsaLaQueYaTieneElPerfil() {
        Perfil perfil = perfil("https://fotos/dev/perfiles/actual.jpg");
        cuentaNueva(perfil);

        servicio.vincular(solicitud(null));

        assertThat(perfil.getFotoUrl()).isEqualTo("https://fotos/dev/perfiles/actual.jpg");
        verify(perfiles, never()).save(any());
        verify(solicitudes).save(any());
    }

    @Test
    void sinFotoNiEnElPerfilDa422() {
        cuentaNueva(perfil(null));

        assertThatThrownBy(() -> servicio.vincular(solicitud(null)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("FOTO_OBLIGATORIA"));
        verify(solicitudes, never()).save(any());
    }

    @Test
    void fotoQueNoEsDelAlmacenDa400() {
        cuentaNueva(perfil(null));
        when(fotos.esDelBucket("https://otro.sitio/foto.jpg")).thenReturn(false);

        assertThatThrownBy(() -> servicio.vincular(solicitud("https://otro.sitio/foto.jpg")))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("FOTO_INVALIDA"));
    }

    @ParameterizedTest
    @CsvSource({"ADMIN_SISTEMA,ES_ADMIN_SISTEMA", "CAJERO,ES_PERSONAL_SUCURSAL", "CHOFER,YA_ES_CHOFER"})
    void cuentasQueNoPuedenSerChofer(String rol, String codigo) {
        Usuario cuenta = ReglasAsignacionRolTest.usuario("ACTIVO", Roles.CLIENTE, rol);
        if (Roles.CHOFER.equals(rol)) {
            cuenta.setChoferId(UUID.randomUUID());
        }
        when(solicitudes.findById(CHOFER_ID)).thenReturn(Optional.empty());
        when(usuarios.findById(cuenta.getId())).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> servicio.vincular(new VincularCuentaRequest(CHOFER_ID, cuenta.getId(), Roles.CHOFER,
                SUCURSAL_ID, null, FOTO)))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
        verifyNoInteractions(perfiles);
        verify(solicitudes, never()).save(any());
    }

    private void cuentaNueva(Perfil perfil) {
        when(solicitudes.findById(CHOFER_ID)).thenReturn(Optional.empty());
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(perfiles.findById(cliente.getId())).thenReturn(Optional.of(perfil));
    }

    private VincularCuentaRequest solicitud(String fotoUrl) {
        return new VincularCuentaRequest(CHOFER_ID, cliente.getId(), Roles.CHOFER, SUCURSAL_ID, null, fotoUrl);
    }

    private static Perfil perfil(String fotoUrl) {
        Perfil perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        perfil.setFotoUrl(fotoUrl);
        return perfil;
    }
}
