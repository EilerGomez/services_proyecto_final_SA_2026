package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.SolicitudCuentaRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentasPersonalServiceImplTest {

    private static final UUID SOLICITUD_ID = UUID.randomUUID();
    private static final UUID SUCURSAL_ID = UUID.randomUUID();

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;
    @Mock private CarteraRepository carteras;
    @Mock private SolicitudCuentaRepository solicitudes;
    @Mock private TokenRecuperacionRepository activaciones;
    @Mock private AlmacenFotos fotos;
    @Mock private UsuarioEventPublisher publicador;

    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();
    @Spy private TokenOpacoService opacos = new TokenOpacoService();

    private CuentasPersonalServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new CuentasPersonalServiceImpl(usuarios, perfiles, roles, carteras,
                solicitudes, activaciones, claves, opacos, fotos, publicador, "http://localhost:5173");
    }

    @Test
    void reintentoDevuelveLaMismaCuentaSinCrearNada() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        SolicitudCuentaPersonal previa = new SolicitudCuentaPersonal();
        previa.setUsuarioId(usuarioId);
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(previa));
        Usuario existente = new Usuario();
        existente.setId(usuarioId);
        existente.setCorreo("chofer@correo.com");
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(existente));

        // Act
        var respuesta = servicio.crear(solicitud());

        // Assert
        assertAll(
                () -> assertEquals(usuarioId, respuesta.usuarioId()),
                () -> assertTrue(!respuesta.creada()),
                () -> verify(usuarios, never()).save(any()),
                () -> verify(publicador, never()).publicarCuentaCreada(
                        any(), any(), any(), any(), any(), any()));
    }

    @Test
    void cuentaNuevaAsignaSucursalYChoferId() {
        // Arrange
        when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.empty());
        when(fotos.esDelBucket(any())).thenReturn(true);
        when(roles.findAllById(Set.of("CHOFER", "CLIENTE")))
                .thenReturn(List.of(rol("CHOFER"), rol("CLIENTE")));
        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);

        // Act
        servicio.crear(solicitud());

        // Assert
        verify(usuarios).save(captor.capture());
        assertAll(
                () -> assertEquals(SUCURSAL_ID, captor.getValue().getSucursalId()),
                () -> assertEquals(SOLICITUD_ID, captor.getValue().getChoferId()),
                () -> verify(solicitudes).save(any(SolicitudCuentaPersonal.class)));
    }

    private CuentaPersonalRequest solicitud() {
        return new CuentaPersonalRequest(SOLICITUD_ID, "CHOFER", SUCURSAL_ID, SOLICITUD_ID,
                "chofer@correo.com", "Pedro", "Ramírez", "9988776-5", "3234567890101",
                "55553333", "Xela", "https://stproyecto2fotos.blob.core.windows.net/fotos/choferes/a.jpg");
    }

    private Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }
}
