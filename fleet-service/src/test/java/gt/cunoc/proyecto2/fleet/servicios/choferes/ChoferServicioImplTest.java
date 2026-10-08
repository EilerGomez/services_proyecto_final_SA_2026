package gt.cunoc.proyecto2.fleet.servicios.choferes;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.clientes.identity.ClienteIdentity;
import gt.cunoc.proyecto2.fleet.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarLicenciaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferInternoRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.DesactivarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.NuevoChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.dtos.trips.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.fleet.errores.choferes.ErrorChofer;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.choferes.ChoferRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.fotos.AlmacenFotosServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChoferServicioImplTest {

    @Mock
    private ChoferRepositorio repositorio;
    @Mock
    private SucursalRepositorio sucursales;
    @Mock
    private AlmacenFotosServicio fotos;
    @Mock
    private ClienteIdentity identity;
    @Mock
    private ClienteTrips trips;
    @Mock
    private PublicadorEventosFleet eventos;

    private ChoferServicioImpl servicio;
    private SucursalEntidad xela;
    private UsuarioActual adminXela;
    private ChoferEntidad chofer;

    @BeforeEach
    void preparar() {
        servicio = new ChoferServicioImpl(repositorio, sucursales, fotos, identity, trips, eventos);
        xela = DatosPrueba.sucursal(true);
        adminXela = DatosPrueba.adminSucursal(xela.getId());
        chofer = DatosPrueba.chofer(xela.getId());
    }

    private void guardarDevuelveElMismo() {
        when(repositorio.saveAndFlush(any(ChoferEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void elChoferExiste() {
        when(repositorio.findById(chofer.getId())).thenReturn(Optional.of(chofer));
    }

    private NuevoChoferSolicitud nuevoChofer(UUID sucursalId, UUID solicitudId, LocalDate vencimiento) {
        return new NuevoChoferSolicitud(sucursalId, "lic-1234", TipoLicencia.A, vencimiento,
                new BigDecimal("350.00"), FECHA, DatosPrueba.cuenta(solicitudId, DatosPrueba.FOTO_CHOFER));
    }

    // ----- F15, F16 y F17 -----

    @Test
    void listarConElFiltroDeSucursal() {
        when(repositorio.buscar(xela.getId(), EstadoChofer.ACTIVO)).thenReturn(List.of(chofer));

        assertThat(servicio.listar(null, EstadoChofer.ACTIVO, adminXela)).containsExactly(chofer);
    }

    @Test
    void obtenerUnChoferQueNoExiste() {
        UUID id = UUID.randomUUID();

        VerificarError.lanza(() -> servicio.obtener(id, adminXela), ErrorChofer.CHOFER_NO_EXISTE);
    }

    @Test
    void obtenerUnChoferDeOtraSucursal() {
        elChoferExiste();
        UsuarioActual adminGuate = DatosPrueba.adminSucursal(UUID.randomUUID());

        assertThatThrownBy(() -> servicio.obtener(chofer.getId(), adminGuate))
                .isInstanceOf(ProhibidoException.class);
    }

    @Test
    void elChoferVeSusPropiosDatos() {
        elChoferExiste();

        assertThat(servicio.obtenerPropio(DatosPrueba.usuarioChofer(chofer.getId()))).isSameAs(chofer);
    }

    @Test
    void unUsuarioSinChoferIdNoTieneDatosDeChofer() {
        VerificarError.lanza(() -> servicio.obtenerPropio(adminXela), ErrorChofer.CHOFER_NO_EXISTE);
        verifyNoInteractions(repositorio);
    }

    // ----- F18 -----

    @Test
    void crearEnOtraSucursalEstaProhibido() {
        NuevoChoferSolicitud solicitud = nuevoChofer(UUID.randomUUID(), null, FECHA.plusYears(1));

        assertThatThrownBy(() -> servicio.crear(solicitud, adminXela)).isInstanceOf(ProhibidoException.class);
        verifyNoInteractions(identity);
    }

    @Test
    void crearEnUnaSucursalInactiva() {
        SucursalEntidad inactiva = DatosPrueba.sucursal(false);
        when(sucursales.findById(inactiva.getId())).thenReturn(Optional.of(inactiva));

        VerificarError.lanza(() -> servicio.crear(nuevoChofer(inactiva.getId(), null, FECHA.plusYears(1)),
                DatosPrueba.adminSistema()), ErrorSucursal.SUCURSAL_INACTIVA);
    }

    @Test
    void crearConLicenciaVencida() {
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));

        VerificarError.lanza(() -> servicio.crear(nuevoChofer(xela.getId(), null, FECHA.minusDays(1)), adminXela),
                ErrorChofer.LICENCIA_VENCIDA);
        verifyNoInteractions(identity);
    }

    @Test
    void crearConLicenciaRepetida() {
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        when(repositorio.existsByNumeroLicencia("LIC-1234")).thenReturn(true);

        VerificarError.lanza(() -> servicio.crear(nuevoChofer(xela.getId(), null, FECHA.plusYears(1)), adminXela),
                ErrorChofer.LICENCIA_EN_USO);
        verifyNoInteractions(identity);
    }

    @Test
    void crearCreaLaCuentaGuardaYPublicaDosEventos() {
        UUID solicitudId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        when(identity.crearCuentaPersonal(any(SolicitudCuentaPersonal.class)))
                .thenReturn(new CuentaPersonalRespuesta(usuarioId, "ana@fleet.gt", "Ana Lopez", true));
        guardarDevuelveElMismo();

        ChoferRespuesta respuesta = servicio.crear(nuevoChofer(xela.getId(), solicitudId, FECHA.plusYears(1)),
                adminXela);

        ArgumentCaptor<SolicitudCuentaPersonal> r5 = ArgumentCaptor.forClass(SolicitudCuentaPersonal.class);
        verify(identity).crearCuentaPersonal(r5.capture());
        assertThat(r5.getValue().rol()).isEqualTo(Roles.CHOFER);
        assertThat(r5.getValue().choferId()).isEqualTo(solicitudId);
        assertThat(respuesta.choferId()).isEqualTo(solicitudId);
        assertThat(respuesta.usuarioId()).isEqualTo(usuarioId);
        assertThat(respuesta.nombreCompleto()).isEqualTo("Ana Lopez");
        assertThat(respuesta.numeroLicencia()).isEqualTo("LIC-1234");
        verify(fotos).verificarUrl(DatosPrueba.FOTO_CHOFER);
        verify(eventos).choferCreado(any(ChoferEntidad.class), eq(FECHA));
        verify(eventos).licenciaChoferActualizada(any(ChoferEntidad.class), eq(FECHA));
    }


    @Test
    void unSolicitudIdUsadoConOtraLicencia() {
        UUID solicitudId = UUID.randomUUID();
        ChoferEntidad existente = DatosPrueba.chofer(solicitudId, xela.getId());
        existente.actualizarLicencia("OTRA-999", TipoLicencia.A, FECHA.plusYears(1));
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(existente));

        VerificarError.lanza(() -> servicio.crear(nuevoChofer(xela.getId(), solicitudId, FECHA.plusYears(1)),
                adminXela), ErrorGeneral.SOLICITUD_YA_USADA);
    }

    // ----- F19 -----

    @Test
    void actualizarSalarioConVersionVieja() {
        elChoferExiste();

        VerificarError.lanza(() -> servicio.actualizar(chofer.getId(),
                new ActualizarChoferSolicitud(new BigDecimal("400.00"), 9), adminXela),
                ErrorGeneral.MODIFICACION_CONCURRENTE);
    }


    // ----- F20 -----

    @Test
    void renovarConUnaLicenciaYaVencida() {
        elChoferExiste();
        ActualizarLicenciaSolicitud solicitud = new ActualizarLicenciaSolicitud("LIC-1234", TipoLicencia.A,
                FECHA.minusDays(1), FECHA, 0);

        VerificarError.lanza(() -> servicio.actualizarLicencia(chofer.getId(), solicitud, adminXela),
                ErrorChofer.LICENCIA_VENCIDA);
    }

    @Test
    void renovarConLaLicenciaDeOtroChofer() {
        elChoferExiste();
        when(repositorio.existsByNumeroLicenciaAndIdNot("LIC-7777", chofer.getId())).thenReturn(true);
        ActualizarLicenciaSolicitud solicitud = new ActualizarLicenciaSolicitud("lic-7777", TipoLicencia.A,
                FECHA.plusYears(3), FECHA, 0);

        VerificarError.lanza(() -> servicio.actualizarLicencia(chofer.getId(), solicitud, adminXela),
                ErrorChofer.LICENCIA_EN_USO);
    }

    @Test
    void renovarLaLicenciaPublicaDosEventos() {
        elChoferExiste();
        guardarDevuelveElMismo();
        ActualizarLicenciaSolicitud solicitud = new ActualizarLicenciaSolicitud("LIC-1234", TipoLicencia.B,
                FECHA.plusYears(3), FECHA, 0);

        ChoferRespuesta respuesta = servicio.actualizarLicencia(chofer.getId(), solicitud, adminXela);

        assertThat(respuesta.tipoLicencia()).isEqualTo(TipoLicencia.B);
        assertThat(respuesta.fechaVencimientoLicencia()).isEqualTo(FECHA.plusYears(3));
        verify(eventos).choferActualizado(chofer, FECHA);
        verify(eventos).licenciaChoferActualizada(chofer, FECHA);
    }


    @Test
    void noSeDesactivaConViajesActivos() {
        elChoferExiste();
        when(trips.viajesActivosDelChofer(chofer.getId())).thenReturn(new ViajesActivosRespuesta(1, List.of()));

        VerificarError.lanza(() -> servicio.desactivar(chofer.getId(), new DesactivarChoferSolicitud(FECHA, 0),
                adminXela), ErrorChofer.CHOFER_CON_VIAJES_ACTIVOS);
        assertThat(chofer.esActivo()).isTrue();
    }

    @Test
    void desactivarSinViajesPublicaChoferDesactivado() {
        elChoferExiste();
        guardarDevuelveElMismo();
        when(trips.viajesActivosDelChofer(chofer.getId())).thenReturn(new ViajesActivosRespuesta(0, null));

        ChoferRespuesta respuesta = servicio.desactivar(chofer.getId(), new DesactivarChoferSolicitud(FECHA, 0),
                adminXela);

        assertThat(respuesta.estado()).isEqualTo(EstadoChofer.INACTIVO);
        verify(eventos).choferDesactivado(chofer, FECHA);
    }


    @Test
    void perfilConElMismoNombreNoPublica() {
        elChoferExiste();

        servicio.actualizarNombreDesdePerfil(chofer.getId(), "Juan Perez", FECHA);

        verify(repositorio, never()).saveAndFlush(any(ChoferEntidad.class));
        verifyNoInteractions(eventos);
    }
}
