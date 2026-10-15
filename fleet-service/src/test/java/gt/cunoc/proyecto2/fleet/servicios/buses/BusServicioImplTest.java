package gt.cunoc.proyecto2.fleet.servicios.buses;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.fleet.dtos.buses.ActualizarBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.BusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.CambiarEstadoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.NuevoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.trips.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.fleet.errores.buses.ErrorBus;
import gt.cunoc.proyecto2.fleet.errores.fotos.ErrorFoto;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusServicioImplTest {

    @Mock
    private BusRepositorio repositorio;
    @Mock
    private SucursalRepositorio sucursales;
    @Mock
    private AlmacenFotosServicio fotos;
    @Mock
    private ClienteTrips trips;
    @Mock
    private PublicadorEventosFleet eventos;

    private BusServicioImpl servicio;
    private SucursalEntidad xela;
    private UsuarioActual adminXela;
    private BusEntidad bus;

    @BeforeEach
    void preparar() {
        servicio = new BusServicioImpl(repositorio, sucursales, fotos, trips, eventos);
        xela = DatosPrueba.sucursal(true);
        adminXela = DatosPrueba.adminSucursal(xela.getId());
        bus = DatosPrueba.bus(xela.getId());
    }

    private void guardarDevuelveElMismo() {
        when(repositorio.saveAndFlush(any(BusEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void elBusExiste() {
        when(repositorio.findById(bus.getId())).thenReturn(Optional.of(bus));
    }

    private NuevoBusSolicitud nuevoBus(UUID sucursalId) {
        return new NuevoBusSolicitud(sucursalId, "p-555xyz", "Volvo", "B12", 2020, 48,
                new BigDecimal("150.00"), DatosPrueba.FOTO_BUS, FECHA);
    }

    private static CambiarEstadoBusSolicitud estado(EstadoBus estado) {
        return new CambiarEstadoBusSolicitud(estado, FECHA, 0);
    }

    // ----- F10 y F11 -----

    @Test
    void listarConElFiltroDeSucursal() {
        when(repositorio.buscar(xela.getId(), null)).thenReturn(List.of(bus));
        when(repositorio.buscar(null, EstadoBus.ACTIVO)).thenReturn(List.of());

        assertThat(servicio.listar(null, null, adminXela)).containsExactly(bus);
        assertThat(servicio.listar(null, EstadoBus.ACTIVO, DatosPrueba.adminSistema())).isEmpty();
    }

    @Test
    void obtenerUnBusQueNoExiste() {
        UUID id = UUID.randomUUID();

        VerificarError.lanza(() -> servicio.obtener(id, adminXela), ErrorBus.BUS_NO_EXISTE);
    }

    @Test
    void obtenerUnBusDeOtraSucursal() {
        elBusExiste();
        UsuarioActual adminGuate = DatosPrueba.adminSucursal(UUID.randomUUID());

        assertThatThrownBy(() -> servicio.obtener(bus.getId(), adminGuate)).isInstanceOf(ProhibidoException.class);
    }

    // ----- F12 -----

    @Test
    void crearEnOtraSucursalEstaProhibido() {
        UUID otra = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.crear(nuevoBus(otra), adminXela)).isInstanceOf(ProhibidoException.class);
        verifyNoInteractions(repositorio, eventos);
    }

    @Test
    void crearEnUnaSucursalQueNoExiste() {
        VerificarError.lanza(() -> servicio.crear(nuevoBus(xela.getId()), adminXela),
                ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    @Test
    void crearEnUnaSucursalInactiva() {
        SucursalEntidad inactiva = DatosPrueba.sucursal(false);
        when(sucursales.findById(inactiva.getId())).thenReturn(Optional.of(inactiva));

        VerificarError.lanza(() -> servicio.crear(nuevoBus(inactiva.getId()), DatosPrueba.adminSistema()),
                ErrorSucursal.SUCURSAL_INACTIVA);
    }

    @Test
    void crearConUnaFotoQueNoEsDelBucket() {
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        doThrow(ErrorFoto.FOTO_INVALIDA.excepcion()).when(fotos).verificarUrl(anyString());

        VerificarError.lanza(() -> servicio.crear(nuevoBus(xela.getId()), adminXela), ErrorFoto.FOTO_INVALIDA);
        verify(repositorio, never()).saveAndFlush(any(BusEntidad.class));
    }

    @Test
    void crearConPlacaRepetida() {
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        when(repositorio.existsByPlaca("P-555XYZ")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(nuevoBus(xela.getId()), adminXela))
                .hasMessage("Ya existe un bus con la placa P-555XYZ");
    }

    @Test
    void crearGuardaYPublicaBusCreado() {
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        guardarDevuelveElMismo();

        BusRespuesta respuesta = servicio.crear(nuevoBus(xela.getId()), adminXela);

        assertThat(respuesta.placa()).isEqualTo("P-555XYZ");
        assertThat(respuesta.estado()).isEqualTo(EstadoBus.ACTIVO);
        assertThat(respuesta.kilometrajeActual()).isEqualByComparingTo("150.00");
        verify(fotos).verificarUrl(DatosPrueba.FOTO_BUS);
        verify(eventos).busCreado(any(BusEntidad.class), eq(FECHA));
    }

    // ----- F13 -----

    @Test
    void actualizarConVersionVieja() {
        elBusExiste();
        ActualizarBusSolicitud solicitud = new ActualizarBusSolicitud("Volvo", "B12", 2018, 50,
                DatosPrueba.FOTO_BUS, 3);

        VerificarError.lanza(() -> servicio.actualizar(bus.getId(), solicitud, adminXela),
                ErrorGeneral.MODIFICACION_CONCURRENTE);
    }

    @Test
    void actualizarGuardaYPublica() {
        elBusExiste();
        guardarDevuelveElMismo();
        ActualizarBusSolicitud solicitud = new ActualizarBusSolicitud("Scania", "K410", 2022, 55,
                DatosPrueba.FOTO_BUS, 0);

        BusRespuesta respuesta = servicio.actualizar(bus.getId(), solicitud, adminXela);

        assertThat(respuesta.marca()).isEqualTo("Scania");
        assertThat(respuesta.capacidadPasajeros()).isEqualTo(55);
        verify(fotos).verificarUrl(DatosPrueba.FOTO_BUS);
        verify(eventos).busActualizado(eq(bus), any(LocalDate.class));
        verify(fotos, never()).eliminarAlConfirmar(anyString());     // misma foto: no se borra nada
    }

    @Test
    void actualizarConOtraFotoBorraLaAnterior() {
        elBusExiste();
        guardarDevuelveElMismo();
        String nueva = DatosPrueba.URL_FOTOS + "dev/buses/nueva.png";
        ActualizarBusSolicitud solicitud = new ActualizarBusSolicitud("Volvo", "B12", 2018, 50, nueva, 0);

        BusRespuesta respuesta = servicio.actualizar(bus.getId(), solicitud, adminXela);

        assertThat(respuesta.fotoUrl()).isEqualTo(nueva);
        verify(fotos).eliminarAlConfirmar(DatosPrueba.FOTO_BUS);
    }

    // ----- F14 -----

    @Test
    void cambiarAlMismoEstadoNoHaceNada() {
        elBusExiste();

        BusRespuesta respuesta = servicio.cambiarEstado(bus.getId(), estado(EstadoBus.ACTIVO), adminXela);

        assertThat(respuesta.estado()).isEqualTo(EstadoBus.ACTIVO);
        verify(repositorio, never()).saveAndFlush(any(BusEntidad.class));
        verifyNoInteractions(eventos, trips);
    }

    @Test
    void enRutaNoSePideAMano() {
        elBusExiste();

        VerificarError.lanza(() -> servicio.cambiarEstado(bus.getId(), estado(EstadoBus.EN_RUTA), adminXela),
                ErrorBus.ESTADO_NO_PERMITIDO);
        verifyNoInteractions(trips);
    }

    @Test
    void unBusEnRutaNoSeCambiaAMano() {
        bus.registrarSalida();
        elBusExiste();

        VerificarError.lanza(() -> servicio.cambiarEstado(bus.getId(), estado(EstadoBus.INACTIVO), adminXela),
                ErrorBus.BUS_EN_RUTA);
        verifyNoInteractions(trips);
    }

    @Test
    void noSeDesactivaConViajesActivos() {
        elBusExiste();
        when(trips.viajesActivosDelBus(bus.getId())).thenReturn(new ViajesActivosRespuesta(2, List.of()));

        assertThatThrownBy(() -> servicio.cambiarEstado(bus.getId(), estado(EstadoBus.INACTIVO), adminXela))
                .hasMessageContaining("2 viaje(s)");
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.ACTIVO);
        verifyNoInteractions(eventos);
    }

    @Test
    void desactivarSinViajesPublicaBusDesactivado() {
        elBusExiste();
        guardarDevuelveElMismo();
        when(trips.viajesActivosDelBus(bus.getId())).thenReturn(new ViajesActivosRespuesta(0, List.of()));

        BusRespuesta respuesta = servicio.cambiarEstado(bus.getId(), estado(EstadoBus.INACTIVO), adminXela);

        assertThat(respuesta.estado()).isEqualTo(EstadoBus.INACTIVO);
        verify(eventos).busDesactivado(bus, FECHA);
    }

    @Test
    void mandarAlTallerNoConsultaTripsYPublicaBusActualizado() {
        elBusExiste();
        guardarDevuelveElMismo();

        BusRespuesta respuesta = servicio.cambiarEstado(bus.getId(), estado(EstadoBus.EN_TALLER), adminXela);

        assertThat(respuesta.estado()).isEqualTo(EstadoBus.EN_TALLER);
        verify(eventos).busActualizado(bus, FECHA);
        verifyNoInteractions(trips);
    }

}
