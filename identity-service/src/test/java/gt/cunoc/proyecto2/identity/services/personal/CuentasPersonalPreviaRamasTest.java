package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
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
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CuentasPersonalServiceImpl.crear con reintento y perfil: usa el nombre del perfil. */
@ExtendWith(MockitoExtension.class)
class CuentasPersonalPreviaRamasTest {

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
                solicitudes, activaciones, claves, opacos, fotos, publicador,
                "http://localhost:5173");
    }

    @Test
    void reintentoConPerfilUsaNombre() {
        UUID solicitudId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        SolicitudCuentaPersonal previa = new SolicitudCuentaPersonal();
        previa.setSolicitudId(solicitudId);
        previa.setUsuarioId(usuarioId);
        when(solicitudes.findById(solicitudId)).thenReturn(Optional.of(previa));
        Usuario existente = new Usuario();
        existente.setId(usuarioId);
        existente.setCorreo("p@correo.com");
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(existente));
        Perfil perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("López");
        when(perfiles.findById(usuarioId)).thenReturn(Optional.of(perfil));

        var respuesta = servicio.crear(new CuentaPersonalRequest(solicitudId, "CAJERO",
                UUID.randomUUID(), null, "p@correo.com", "Ana", "López", "CF", "1234567890123",
                "12345678", "Ciudad", null));

        assertAll(
                () -> assertEquals("Ana López", respuesta.nombreCompleto()),
                () -> assertEquals(false, respuesta.creada()),
                () -> verify(usuarios, never()).save(any()));
    }
}
