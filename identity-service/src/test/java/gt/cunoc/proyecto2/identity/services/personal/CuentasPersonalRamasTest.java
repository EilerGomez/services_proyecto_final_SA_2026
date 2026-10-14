package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas borde de R5 no cubiertas por CuentasPersonalServiceImplTest. */
@ExtendWith(MockitoExtension.class)
class CuentasPersonalRamasTest {

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

    private CuentaPersonalRequest base(String rol) {
        return new CuentaPersonalRequest(UUID.randomUUID(), rol, UUID.randomUUID(), null,
                "p@correo.com", "Ana", "López", "CF", "1234567890123", "12345678", "Ciudad", null);
    }

    private CuentaPersonalRequest chofer(String fotoUrl) {
        return new CuentaPersonalRequest(UUID.randomUUID(), "CHOFER", UUID.randomUUID(),
                UUID.randomUUID(), "c@correo.com", "Juan", "Pérez", "CF", "1234567890123",
                "12345678", "Ciudad", fotoUrl);
    }

    @Test
    void reintentoSinUsuarioDa422() {
        UUID solicitudId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        SolicitudCuentaPersonal previa = new SolicitudCuentaPersonal();
        previa.setSolicitudId(solicitudId);
        previa.setUsuarioId(usuarioId);
        when(solicitudes.findById(solicitudId)).thenReturn(Optional.of(previa));
        when(usuarios.findById(usuarioId)).thenReturn(Optional.empty());
        var solicitud = new CuentaPersonalRequest(solicitudId, "CAJERO", UUID.randomUUID(), null,
                "p@correo.com", "Ana", "López", "CF", "1234567890123", "12345678", "Ciudad", null);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(solicitud));
        assertEquals("CUENTA_INCONSISTENTE", error.getCodigo());
    }

    @Test
    void reintentoSinPerfilUsaCorreo() {
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
        when(perfiles.findById(usuarioId)).thenReturn(Optional.empty());

        var respuesta = servicio.crear(new CuentaPersonalRequest(solicitudId, "CAJERO",
                UUID.randomUUID(), null, "p@correo.com", "Ana", "López", "CF", "1234567890123",
                "12345678", "Ciudad", null));

        assertEquals("p@correo.com", respuesta.nombreCompleto());
        assertEquals(false, respuesta.creada());
        verify(usuarios, never()).save(any());
    }

    @Test
    void choferSinIdDa400() {
        var solicitud = new CuentaPersonalRequest(UUID.randomUUID(), "CHOFER", UUID.randomUUID(),
                null, "c@correo.com", "Juan", "Pérez", "CF", "1234567890123", "12345678",
                "Ciudad", "https://foto.jpg");

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud));
        assertEquals("CHOFER_SIN_ID", error.getCodigo());
    }

    @Test
    void choferSinFotoDa422() {
        ReglaNegocioException nula = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(chofer(null)));
        ReglaNegocioException blanca = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(chofer("   ")));
        assertEquals("FOTO_OBLIGATORIA", nula.getCodigo());
        assertEquals("FOTO_OBLIGATORIA", blanca.getCodigo());
    }

    @Test
    void fotoExternaDa400() {
        when(fotos.esDelBucket("https://otro.com/foto.jpg")).thenReturn(false);

        var solicitud = new CuentaPersonalRequest(UUID.randomUUID(), "CAJERO", UUID.randomUUID(),
                null, "p@correo.com", "Ana", "López", "CF", "1234567890123", "12345678",
                "Ciudad", "https://otro.com/foto.jpg");

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud));
        assertEquals("FOTO_INVALIDA", error.getCodigo());
    }

    @Test
    void correoDuplicadoDa409() {
        when(usuarios.existsByCorreo("p@correo.com")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crear(base("CAJERO")));
        assertEquals("CORREO_EN_USO", error.getCodigo());
    }

    @Test
    void nitDuplicadoDa409() {
        when(usuarios.existsByCorreo("p@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crear(base("CAJERO")));
        assertEquals("NIT_EN_USO", error.getCodigo());
    }

    @Test
    void dpiDuplicadoDa409() {
        when(usuarios.existsByCorreo("p@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crear(base("CAJERO")));
        assertEquals("DPI_EN_USO", error.getCodigo());
    }

    @Test
    void rolesFaltantesDa422() {
        when(usuarios.existsByCorreo("p@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(false);
        Rol cajero = new Rol();
        cajero.setCodigo("CAJERO");
        when(roles.findAllById(Set.of("CAJERO", "CLIENTE"))).thenReturn(List.of(cajero));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(base("CAJERO")));
        assertEquals("ROL_FALTANTE", error.getCodigo());
    }

    @Test
    void cuentaCajeroNoPideChoferIdNiFoto() {
        when(usuarios.existsByCorreo("p@correo.com")).thenReturn(false);
        when(perfiles.existsByNit("CF")).thenReturn(false);
        when(perfiles.existsByDpi("1234567890123")).thenReturn(false);
        Rol cajero = new Rol();
        cajero.setCodigo("CAJERO");
        Rol cliente = new Rol();
        cliente.setCodigo("CLIENTE");
        when(roles.findAllById(Set.of("CAJERO", "CLIENTE")))
                .thenReturn(List.of(cajero, cliente));

        var respuesta = servicio.crear(base("CAJERO"));

        assertTrue(respuesta.creada());
        verify(usuarios).save(any(Usuario.class));
        verify(perfiles).save(any(Perfil.class));
    }
}
