package gt.cunoc.proyecto2.fleet.servicios.sucursales;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalCreadaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
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
class SucursalServicioImplTest {

    @Mock
    private SucursalRepositorio repositorio;
    @Mock
    private PersonalSucursalServicio personal;
    @Mock
    private BusRepositorio buses;
    @Mock
    private PublicadorEventosFleet eventos;

    private SucursalServicioImpl servicio;
    private SucursalEntidad sucursal;

    @BeforeEach
    void preparar() {
        servicio = new SucursalServicioImpl(repositorio, personal, buses, eventos);
        sucursal = DatosPrueba.sucursal(true);
    }

    private void guardarDevuelveLaMisma() {
        when(repositorio.saveAndFlush(any(SucursalEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static NuevaSucursalSolicitud nuevaSucursal(DatosCuentaSolicitud admin) {
        return new NuevaSucursalSolicitud("XELA", "Xela Central", "Quetzaltenango", "Quetzaltenango",
                "4a calle", null, new BigDecimal("14.83"), new BigDecimal("-91.51"), FECHA, admin);
    }

    private static ActualizarSucursalSolicitud actualizacion(String nombre, int version) {
        return new ActualizarSucursalSolicitud(nombre, "Quetzaltenango", "Quetzaltenango", "5a avenida",
                null, new BigDecimal("14.83"), new BigDecimal("-91.51"), version);
    }

    // ----- F1 y F2 -----

    @Test
    void listarSinFiltroDevuelveTodas() {
        when(repositorio.findAllByOrderByNombreAsc()).thenReturn(List.of(sucursal));

        assertThat(servicio.listar(null)).containsExactly(sucursal);
        verify(repositorio, never()).findByActivaOrderByNombreAsc(anyBoolean());
    }

    @Test
    void listarPorEstado() {
        when(repositorio.findByActivaOrderByNombreAsc(false)).thenReturn(List.of());

        assertThat(servicio.listar(false)).isEmpty();
    }

    @Test
    void obtenerUnaSucursalQueNoExiste() {
        UUID id = UUID.randomUUID();

        VerificarError.lanza(() -> servicio.obtenerPorId(id), ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    // ----- F3 -----

    @Test
    void crearConCodigoRepetido() {
        when(repositorio.existsByCodigo("XELA")).thenReturn(true);

        VerificarError.lanza(() -> servicio.crear(nuevaSucursal(DatosPrueba.cuenta(null, null))),
                ErrorSucursal.SUCURSAL_DUPLICADA);
        verifyNoInteractions(personal, eventos);
    }

    @Test
    void crearConNombreRepetido() {
        when(repositorio.existsByNombre("Xela Central")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(nuevaSucursal(DatosPrueba.cuenta(null, null))))
                .hasMessage("Ya existe una sucursal con el nombre Xela Central");
    }

    @Test
    void crearGuardaLaSucursalPublicaYRegistraAlAdminInicial() {
        DatosCuentaSolicitud admin = DatosPrueba.cuenta(UUID.randomUUID(), null);
        guardarDevuelveLaMisma();
        when(personal.registrarAlta(any(SucursalEntidad.class), eq(RolPersonal.ADMIN_SUCURSAL), eq(FECHA), eq(admin)))
                .thenAnswer(i -> DatosPrueba.personal(((SucursalEntidad) i.getArgument(0)).getId(),
                        RolPersonal.ADMIN_SUCURSAL));

        SucursalCreadaRespuesta respuesta = servicio.crear(nuevaSucursal(admin));

        ArgumentCaptor<SucursalEntidad> guardada = ArgumentCaptor.forClass(SucursalEntidad.class);
        verify(eventos).sucursalCreada(guardada.capture(), eq(FECHA));
        assertThat(respuesta.sucursal().sucursalId()).isEqualTo(guardada.getValue().getId());
        assertThat(respuesta.sucursal().codigo()).isEqualTo("XELA");
        assertThat(respuesta.sucursal().activa()).isTrue();
        assertThat(respuesta.adminInicial().sucursalId()).isEqualTo(guardada.getValue().getId());
        assertThat(respuesta.adminInicial().rol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
    }

    // ----- F4 -----

    @Test
    void actualizarConVersionVieja() {
        when(repositorio.findById(sucursal.getId())).thenReturn(Optional.of(sucursal));

        VerificarError.lanza(() -> servicio.actualizar(sucursal.getId(), actualizacion("Xela", 5)),
                ErrorGeneral.MODIFICACION_CONCURRENTE);
        verify(repositorio, never()).saveAndFlush(any(SucursalEntidad.class));
    }

    @Test
    void actualizarConNombreDeOtraSucursal() {
        when(repositorio.findById(sucursal.getId())).thenReturn(Optional.of(sucursal));
        when(repositorio.existsByNombreAndIdNot("Guate Central", sucursal.getId())).thenReturn(true);

        VerificarError.lanza(() -> servicio.actualizar(sucursal.getId(), actualizacion("Guate Central", 0)),
                ErrorSucursal.SUCURSAL_DUPLICADA);
    }

    @Test
    void actualizarGuardaYPublica() {
        when(repositorio.findById(sucursal.getId())).thenReturn(Optional.of(sucursal));
        guardarDevuelveLaMisma();

        SucursalRespuesta respuesta = servicio.actualizar(sucursal.getId(), actualizacion("Xela Norte", 0));

        assertThat(respuesta.nombre()).isEqualTo("Xela Norte");
        assertThat(respuesta.direccion()).isEqualTo("5a avenida");
        verify(eventos).sucursalActualizada(eq(sucursal), any(LocalDate.class));
    }

    // ----- F5 -----

    @Test
    void cambiarAlMismoEstadoNoGuardaNiPublica() {
        when(repositorio.findById(sucursal.getId())).thenReturn(Optional.of(sucursal));

        SucursalRespuesta respuesta = servicio.cambiarEstado(sucursal.getId(),
                new CambiarEstadoSucursalSolicitud(true, 0));

        assertThat(respuesta.activa()).isTrue();
        verify(repositorio, never()).saveAndFlush(any(SucursalEntidad.class));
        verifyNoInteractions(eventos, buses);
    }

    @Test
    void noSeDesactivaConBusesQueNoEstanInactivos() {
        when(repositorio.findById(sucursal.getId())).thenReturn(Optional.of(sucursal));
        when(buses.existsBySucursalIdAndEstadoNot(sucursal.getId(), EstadoBus.INACTIVO)).thenReturn(true);

        VerificarError.lanza(() -> servicio.cambiarEstado(sucursal.getId(),
                new CambiarEstadoSucursalSolicitud(false, 0)), ErrorSucursal.SUCURSAL_CON_BUSES_ACTIVOS);
        verifyNoInteractions(eventos);
    }


}
