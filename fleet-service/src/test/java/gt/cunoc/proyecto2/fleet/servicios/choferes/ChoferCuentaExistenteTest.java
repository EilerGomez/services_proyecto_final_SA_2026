package gt.cunoc.proyecto2.fleet.servicios.choferes;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.clientes.identity.ClienteIdentity;
import gt.cunoc.proyecto2.fleet.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.NuevoChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.UsuarioExistenteSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.VinculoCuentaPersonal;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.choferes.ChoferRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.fotos.AlmacenFotosServicio;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** F18 con una persona que ya tiene cuenta (R5b): no se crea cuenta y el rol llega con ChoferCreado. */
@ExtendWith(MockitoExtension.class)
class ChoferCuentaExistenteTest {

    @Mock private ChoferRepositorio repositorio;
    @Mock private SucursalRepositorio sucursales;
    @Mock private AlmacenFotosServicio fotos;
    @Mock private ClienteIdentity identity;
    @Mock private ClienteTrips trips;
    @Mock private PublicadorEventosFleet eventos;

    private ChoferServicioImpl servicio;
    private SucursalEntidad xela;
    private UsuarioActual adminXela;
    private final UUID solicitudId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        servicio = new ChoferServicioImpl(repositorio, sucursales, fotos, identity, trips, eventos);
        xela = DatosPrueba.sucursal(true);
        adminXela = DatosPrueba.adminSucursal(xela.getId());
    }

    private NuevoChoferSolicitud conCuentaExistente(String fotoUrl) {
        return new NuevoChoferSolicitud(xela.getId(), "lic-1234", TipoLicencia.A, FECHA.plusYears(1),
                new BigDecimal("350.00"), FECHA, null, new UsuarioExistenteSolicitud(solicitudId, usuarioId, fotoUrl));
    }

    private void identityValida() {
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        when(identity.vincularCuentaExistente(any(VinculoCuentaPersonal.class)))
                .thenReturn(new CuentaPersonalRespuesta(usuarioId, "ana@correo.com", "Ana López", false));
        when(repositorio.saveAndFlush(any(ChoferEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void validaLaCuentaEnIdentityGuardaElChoferYPublicaChoferCreado() {
        identityValida();

        ChoferRespuesta respuesta = servicio.crear(conCuentaExistente(null), adminXela);

        verify(identity).vincularCuentaExistente(
                VinculoCuentaPersonal.paraChofer(solicitudId, usuarioId, xela.getId(), xela.getNombre(), null));
        verify(identity, never()).crearCuentaPersonal(any());
        verify(fotos, never()).verificarUrl(any());       // sin foto nueva: Identity usa la de su perfil
        assertThat(respuesta.choferId()).isEqualTo(solicitudId);
        assertThat(respuesta.usuarioId()).isEqualTo(usuarioId);
        assertThat(respuesta.nombreCompleto()).isEqualTo("Ana López");
        verify(eventos).choferCreado(any(ChoferEntidad.class), eq(FECHA));    // Identity le pone el rol
        verify(eventos).licenciaChoferActualizada(any(ChoferEntidad.class), eq(FECHA));
    }

    @Test
    void conFotoNuevaLaVerificaYSeLaPasaAIdentity() {
        identityValida();

        servicio.crear(conCuentaExistente(DatosPrueba.FOTO_CHOFER), adminXela);

        verify(fotos).verificarUrl(DatosPrueba.FOTO_CHOFER);
        verify(identity).vincularCuentaExistente(VinculoCuentaPersonal.paraChofer(solicitudId, usuarioId,
                xela.getId(), xela.getNombre(), DatosPrueba.FOTO_CHOFER));
    }

    @Test
    void cuentaNuevaYExistenteALaVezDa400() {
        NuevoChoferSolicitud ambas = new NuevoChoferSolicitud(xela.getId(), "lic-1234", TipoLicencia.A,
                FECHA.plusYears(1), new BigDecimal("350.00"), FECHA, DatosPrueba.cuenta(null, DatosPrueba.FOTO_CHOFER),
                new UsuarioExistenteSolicitud(null, usuarioId));

        VerificarError.lanza(() -> servicio.crear(ambas, adminXela), ErrorPersonal.CUENTA_REQUERIDA);
        verifyNoInteractions(identity, repositorio);
    }

    @Test
    void reintentoConOtraCuentaDa409() {
        ChoferEntidad yaGuardado = DatosPrueba.chofer(solicitudId, xela.getId());     // otro usuarioId
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(yaGuardado));

        VerificarError.lanza(() -> servicio.crear(conCuentaExistente(null), adminXela),
                ErrorGeneral.SOLICITUD_YA_USADA);
        verifyNoInteractions(identity);
    }
}
