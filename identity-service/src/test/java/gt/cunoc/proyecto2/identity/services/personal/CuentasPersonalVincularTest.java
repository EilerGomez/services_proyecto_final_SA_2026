package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
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

/** R5b: validar una cuenta existente para personal de sucursal (el rol llega con el evento de Fleet). */
@ExtendWith(MockitoExtension.class)
class CuentasPersonalVincularTest {

    private static final UUID SOLICITUD_ID = UUID.randomUUID();
    private static final UUID SUCURSAL_ID = UUID.randomUUID();

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

    @BeforeEach
    void preparar() {
        servicio = new CuentasPersonalServiceImpl(usuarios, perfiles, roles, solicitudes, activaciones,
                claves, opacos, fotos, publicador, "http://localhost:5173");
    }

    @Test
    void validaLaCuentaYRegistraLaSolicitudSinCambiarRoles() {
        // Arrange
        Usuario cliente = ReglasAsignacionRolTest.usuario("ACTIVO", Roles.CLIENTE);
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.empty());
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(perfiles.findById(cliente.getId())).thenReturn(Optional.of(perfil("Ana", "López")));

        // Act
        CuentaPersonalResponse respuesta = servicio.vincular(solicitud(cliente.getId(), Roles.ADMIN_SUCURSAL));

        // Assert: el rol y la sucursal llegan después con el evento de Fleet (y ahí se avisa)
        assertThat(respuesta).isEqualTo(new CuentaPersonalResponse(cliente.getId(), "ana@correo.com", "Ana López", false));
        assertThat(cliente.getSucursalId()).isNull();
        assertThat(cliente.getRoles()).extracting(Rol::getCodigo).containsExactly(Roles.CLIENTE);
        ArgumentCaptor<SolicitudCuentaPersonal> registro = ArgumentCaptor.forClass(SolicitudCuentaPersonal.class);
        verify(solicitudes).save(registro.capture());
        assertThat(registro.getValue().getSolicitudId()).isEqualTo(SOLICITUD_ID);
        assertThat(registro.getValue().getUsuarioId()).isEqualTo(cliente.getId());
        assertThat(registro.getValue().getRolCodigo()).isEqualTo(Roles.ADMIN_SUCURSAL);
        assertThat(registro.getValue().getSucursalId()).isEqualTo(SUCURSAL_ID);
        verify(usuarios, never()).save(any());
        verifyNoInteractions(publicador, roles, activaciones, claves, opacos);
    }

    @Test
    void reintentoDevuelveLaMismaCuenta() {
        // Arrange
        Usuario cliente = ReglasAsignacionRolTest.usuario("ACTIVO", Roles.CLIENTE, Roles.CAJERO);
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(previa(cliente.getId(), Roles.CAJERO)));
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(perfiles.findById(cliente.getId())).thenReturn(Optional.empty());

        // Act
        CuentaPersonalResponse respuesta = servicio.vincular(solicitud(cliente.getId(), Roles.CAJERO));

        // Assert: sin perfil el nombre es el correo; no se vuelve a validar (ya es cajero)
        assertThat(respuesta.nombreCompleto()).isEqualTo("ana@correo.com");
        verify(solicitudes, never()).save(any());
        verifyNoInteractions(publicador);
    }

    @Test
    void reintentoTrasUnaReversionVuelveAValidar() {
        // Fleet revirtió: la solicitud quedó, pero la cuenta nunca fue personal y luego la promovieron
        Usuario admin = ReglasAsignacionRolTest.usuario("ACTIVO", Roles.CLIENTE, Roles.ADMIN_SISTEMA);
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(previa(admin.getId(), Roles.CAJERO)));
        when(usuarios.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> servicio.vincular(solicitud(admin.getId(), Roles.CAJERO)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ES_ADMIN_SISTEMA"));
    }

    @Test
    void reintentoConOtraCuentaDa409() {
        // Arrange
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(previa(UUID.randomUUID(), Roles.CAJERO)));

        // Act + Assert
        assertThatThrownBy(() -> servicio.vincular(solicitud(UUID.randomUUID(), Roles.CAJERO)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SOLICITUD_YA_USADA"));
    }

    @Test
    void reintentoConOtroRolDa409() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(previa(usuarioId, Roles.CAJERO)));

        // Act + Assert
        assertThatThrownBy(() -> servicio.vincular(solicitud(usuarioId, Roles.ADMIN_SUCURSAL)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SOLICITUD_YA_USADA"));
    }

    @Test
    void reintentoEnOtraSucursalDa409() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        SolicitudCuentaPersonal previa = previa(usuarioId, Roles.CAJERO);
        previa.setSucursalId(UUID.randomUUID());
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(previa));

        // Act + Assert
        assertThatThrownBy(() -> servicio.vincular(solicitud(usuarioId, Roles.CAJERO)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SOLICITUD_YA_USADA"));
    }

    @Test
    void usuarioInexistenteDa422() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.empty());
        when(usuarios.findById(usuarioId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> servicio.vincular(solicitud(usuarioId, Roles.CAJERO)))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("USUARIO_NO_EXISTE");
                    assertThat(e.getStatus().value()).isEqualTo(422);
                });
    }

    @Test
    void personalDeOtraSucursalNoSeVincula() {
        // Arrange
        Usuario cajero = ReglasAsignacionRolTest.usuario("ACTIVO", Roles.CLIENTE, Roles.CAJERO);
        cajero.setSucursalId(UUID.randomUUID());
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.empty());
        when(usuarios.findById(cajero.getId())).thenReturn(Optional.of(cajero));

        // Act + Assert
        assertThatThrownBy(() -> servicio.vincular(solicitud(cajero.getId(), Roles.ADMIN_SUCURSAL)))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("YA_ES_PERSONAL"));
        verify(usuarios, never()).save(any());
        verifyNoInteractions(publicador);
    }

    private static VincularCuentaRequest solicitud(UUID usuarioId, String rol) {
        return new VincularCuentaRequest(SOLICITUD_ID, usuarioId, rol, SUCURSAL_ID, "Xela Central");
    }

    private static SolicitudCuentaPersonal previa(UUID usuarioId, String rol) {
        SolicitudCuentaPersonal previa = new SolicitudCuentaPersonal();
        previa.setSolicitudId(SOLICITUD_ID);
        previa.setUsuarioId(usuarioId);
        previa.setRolCodigo(rol);
        previa.setSucursalId(SUCURSAL_ID);
        return previa;
    }

    private static Perfil perfil(String nombres, String apellidos) {
        Perfil perfil = new Perfil();
        perfil.setNombres(nombres);
        perfil.setApellidos(apellidos);
        return perfil;
    }
}
