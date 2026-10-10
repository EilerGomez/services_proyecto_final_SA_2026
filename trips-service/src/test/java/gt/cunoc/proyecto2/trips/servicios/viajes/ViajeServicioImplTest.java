package gt.cunoc.proyecto2.trips.servicios.viajes;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.LLEGADA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.dtos.viajes.ActualizarViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.dtos.viajes.NuevoViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.VendibleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.trips.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.MotivoNoVendible;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.buses.VistaBusRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.choferes.VistaChoferRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.LlegadaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.SalidaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.rutas.RutaRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.rutas.RutaServicio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class ViajeServicioImplTest {

    private final ViajeRepositorio repositorio = mock(ViajeRepositorio.class);
    private final RutaServicio rutas = mock(RutaServicio.class);
    private final RutaRepositorio rutaRepositorio = mock(RutaRepositorio.class);
    private final VistaSucursalServicio sucursales = mock(VistaSucursalServicio.class);
    private final VistaBusRepositorio buses = mock(VistaBusRepositorio.class);
    private final VistaChoferRepositorio choferes = mock(VistaChoferRepositorio.class);
    private final AsignacionViajeServicio asignaciones = mock(AsignacionViajeServicio.class);
    private final SalidaViajeRepositorio salidas = mock(SalidaViajeRepositorio.class);
    private final LlegadaViajeRepositorio llegadas = mock(LlegadaViajeRepositorio.class);
    private final AlquilerPrivadoRepositorio alquileres = mock(AlquilerPrivadoRepositorio.class);
    private final PublicadorEventosTrips eventos = mock(PublicadorEventosTrips.class);

    private final ViajeServicioImpl servicio = new ViajeServicioImpl(repositorio, rutas, rutaRepositorio,
            sucursales, buses, choferes, asignaciones, salidas, llegadas, alquileres, eventos);

    /* Ruta Xela → Guate con un viaje REGULAR PROGRAMADO (version 0). */
    private final RutaEntidad ruta = DatosPrueba.ruta();
    private final ViajeEntidad viaje = DatosPrueba.viajeRegular(ruta, DatosPrueba.asignacion());
    private final UsuarioActual adminXela = DatosPrueba.adminSucursal(XELA);

    private void elViajeExiste(ViajeEntidad existente) {
        when(repositorio.findById(existente.getId())).thenReturn(Optional.of(existente));
    }

    private void guardarDevuelveElMismo() {
        when(repositorio.saveAndFlush(any(ViajeEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    /* Viaje PRIVADO de un alquiler ASIGNADO de Xela; queda guardado en el repositorio. */
    private ViajeEntidad viajePrivadoDe(AlquilerPrivadoEntidad alquiler) {
        ViajeEntidad privado = DatosPrueba.viajePrivado(alquiler, DatosPrueba.asignacion());
        elViajeExiste(privado);
        return privado;
    }

    private static AlquilerPrivadoEntidad alquilerAsignado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        alquiler.asignar();
        return alquiler;
    }

    private static FiltroViajes sinFiltros() {
        return new FiltroViajes(null, null, null, null, null, null, null);
    }


    @Test
    void buscarSinResultados() {
        when(repositorio.findAll(ArgumentMatchers.<Specification<ViajeEntidad>>any(), any(Sort.class)))
                .thenReturn(List.of());

        assertThat(servicio.buscar(sinFiltros())).isEmpty();
    }

    // T9: obtener un viaje que no existe

    @Test
    void obtenerUnViajeQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.obtener(id, adminXela), ErrorViaje.VIAJE_NO_EXISTE);
    }

    @Test
    void unViajeRegularLoVeCualquieraSinSesion() {
        elViajeExiste(viaje);

        ViajeDetalleRespuesta detalle = servicio.obtener(viaje.getId(), null);

        assertThat(detalle.viaje().viajeId()).isEqualTo(viaje.getId());
        assertThat(detalle.viaje().estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(detalle.salida()).isNull();
        assertThat(detalle.llegada()).isNull();
    }

    @Test
    void unViajePrivadoLoVeElAdminDelSistema() {
        ViajeEntidad privado = viajePrivadoDe(alquilerAsignado());

        ViajeDetalleRespuesta detalle = servicio.obtener(privado.getId(), DatosPrueba.adminSistema());

        assertThat(detalle.viaje().tipo()).isEqualTo(TipoViaje.PRIVADO);
    }

    @Test
    void unViajePrivadoLoVeElPersonalDeSuSucursal() {
        ViajeEntidad privado = viajePrivadoDe(alquilerAsignado());

        assertThat(servicio.obtener(privado.getId(), adminXela).viaje().viajeId()).isEqualTo(privado.getId());
        assertThat(servicio.obtener(privado.getId(), DatosPrueba.cajero(XELA)).viaje().viajeId())
                .isEqualTo(privado.getId());
    }

    @Test
    void unViajePrivadoLoVeSuChoferAsignado() {
        ViajeEntidad privado = viajePrivadoDe(alquilerAsignado());

        ViajeDetalleRespuesta detalle = servicio.obtener(privado.getId(), DatosPrueba.chofer(privado.getChoferId()));

        assertThat(detalle.viaje().choferId()).isEqualTo(privado.getChoferId());
    }

    @Test
    void unViajePrivadoSinSesionEstaProhibido() {
        ViajeEntidad privado = viajePrivadoDe(alquilerAsignado());

        VerificarError.lanza(() -> servicio.obtener(privado.getId(), null), ErrorViaje.VIAJE_PRIVADO);
    }

    @Test
    void unViajePrivadoNoLoVenLosDemas() {
        ViajeEntidad privado = viajePrivadoDe(alquilerAsignado());

        VerificarError.lanza(() -> servicio.obtener(privado.getId(), DatosPrueba.cliente()),
                ErrorViaje.VIAJE_PRIVADO);
        VerificarError.lanza(() -> servicio.obtener(privado.getId(), DatosPrueba.adminSucursal(GUATE)),
                ErrorViaje.VIAJE_PRIVADO);
        VerificarError.lanza(() -> servicio.obtener(privado.getId(), DatosPrueba.chofer(UUID.randomUUID())),
                ErrorViaje.VIAJE_PRIVADO);
    }

    // detalle de un viaje

    @Test
    void detalleConSalidaPeroSinLlegada() {
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
        when(salidas.findById(viaje.getId())).thenReturn(Optional.of(salida));
        when(llegadas.findById(viaje.getId())).thenReturn(Optional.empty());

        ViajeDetalleRespuesta detalle = servicio.detalle(viaje);

        assertThat(detalle.salida()).isNotNull();
        assertThat(detalle.salida().kmInicial()).isEqualByComparingTo("1000.00");
        assertThat(detalle.salida().fechaHoraSalida()).isAtSameInstantAs(SALIDA.plusMinutes(5));
        assertThat(detalle.salida().registradoPor()).isEqualTo(salida.getRegistradoPor());
        assertThat(detalle.llegada()).isNull();
    }

    @Test
    void detalleConSalidaYLlegada() {
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
        LlegadaViajeEntidad llegada = DatosPrueba.llegada(viaje, salida);
        when(salidas.findById(viaje.getId())).thenReturn(Optional.of(salida));
        when(llegadas.findById(viaje.getId())).thenReturn(Optional.of(llegada));

        ViajeDetalleRespuesta detalle = servicio.detalle(viaje);

        assertThat(detalle.viaje().viajeId()).isEqualTo(viaje.getId());
        assertThat(detalle.salida()).isNotNull();
        assertThat(detalle.llegada()).isNotNull();
        assertThat(detalle.llegada().fechaHoraLlegada()).isAtSameInstantAs(LLEGADA.plusMinutes(10));
        assertThat(detalle.llegada().kmRecorridos()).isEqualByComparingTo("207.50");
        assertThat(detalle.llegada().costoDepreciacion()).isEqualByComparingTo("259.38");
        assertThat(detalle.llegada().salarioChofer()).isEqualByComparingTo("350.00");
    }

    //obtenerPorId 

    @Test
    void obtenerPorIdDevuelveLaEntidad() {
        elViajeExiste(viaje);

        assertThat(servicio.obtenerPorId(viaje.getId())).isSameAs(viaje);
    }

    @Test
    void obtenerPorIdDeUnViajeQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.obtenerPorId(id), ErrorViaje.VIAJE_NO_EXISTE);
    }

    //T10: programar 

    private NuevoViajeSolicitud nuevoViaje(UUID busId, UUID choferId) {
        return new NuevoViajeSolicitud(ruta.getId(), busId, choferId, SALIDA, LLEGADA);
    }

    @Test
    void programarEnUnaRutaDeOtraSucursal() {
        when(rutas.obtenerPorId(ruta.getId())).thenReturn(ruta);
        UsuarioActual adminGuate = DatosPrueba.adminSucursal(GUATE);

        VerificarError.lanza(() -> servicio.programar(nuevoViaje(UUID.randomUUID(), UUID.randomUUID()), adminGuate),
                "SUCURSAL_AJENA");
        verifyNoInteractions(asignaciones, eventos);
        verify(repositorio, never()).saveAndFlush(any(ViajeEntidad.class));
    }

    @Test
    void programarEnUnaRutaInactiva() {
        ruta.actualizar(new BigDecimal("205.00"), DatosPrueba.PRECIO_BOLETO, false);
        when(rutas.obtenerPorId(ruta.getId())).thenReturn(ruta);

        VerificarError.lanza(() -> servicio.programar(nuevoViaje(UUID.randomUUID(), UUID.randomUUID()), adminXela),
                ErrorViaje.RUTA_INACTIVA);
        verifyNoInteractions(asignaciones, eventos);
    }

    // T11: actualizar 

    private ActualizarViajeSolicitud cambio(UUID busId, int version) {
        return new ActualizarViajeSolicitud(busId, viaje.getChoferId(), SALIDA, LLEGADA, version);
    }

    @Test
    void actualizarUnViajeDeOtraSucursal() {
        elViajeExiste(viaje);

        VerificarError.lanza(() -> servicio.actualizar(viaje.getId(), cambio(viaje.getBusId(), 0),
                DatosPrueba.adminSucursal(GUATE)), "SUCURSAL_AJENA");
        verifyNoInteractions(asignaciones, eventos);
    }

    @Test
    void actualizarUnViajeQueYaInicioAunqueLaVersionTambienCambio() {
        viaje.iniciar();
        elViajeExiste(viaje);

        // version vieja (7 no es igual a 0), pero gana VIAJE_YA_INICIO porque se revisa primero
        VerificarError.lanza(() -> servicio.actualizar(viaje.getId(), cambio(viaje.getBusId(), 7), adminXela),
                ErrorViaje.VIAJE_YA_INICIO);
        verifyNoInteractions(asignaciones, eventos);
    }

    @Test
    void actualizarConVersionVieja() {
        elViajeExiste(viaje);

        VerificarError.lanza(() -> servicio.actualizar(viaje.getId(), cambio(viaje.getBusId(), 3), adminXela),
                ErrorGeneral.MODIFICACION_CONCURRENTE);
        verifyNoInteractions(asignaciones, eventos);
        verify(repositorio, never()).saveAndFlush(any(ViajeEntidad.class));
    }


    // T12: eliminar

    @Test
    void eliminarUnViajeRegular() {
        elViajeExiste(viaje);

        servicio.eliminar(viaje.getId(), "Se canceló la salida", adminXela);

        InOrder orden = inOrder(repositorio, eventos);
        orden.verify(repositorio).delete(viaje);
        orden.verify(repositorio).flush();
        orden.verify(eventos).viajeEliminado(viaje, "Se canceló la salida");
        verifyNoInteractions(alquileres);
    }

    @Test
    void eliminarUnViajeQueYaInicio() {
        viaje.iniciar();
        elViajeExiste(viaje);

        VerificarError.lanza(() -> servicio.eliminar(viaje.getId(), "motivo", adminXela),
                ErrorViaje.VIAJE_YA_INICIO);
        verify(repositorio, never()).delete(any(ViajeEntidad.class));
        verifyNoInteractions(eventos);
    }

    // R2: consultarVendible 

    @Test
    void unViajeRegularProgramadoEsVendible() {
        elViajeExiste(viaje);

        VendibleRespuesta respuesta = servicio.consultarVendible(viaje.getId());

        assertThat(respuesta.viajeId()).isEqualTo(viaje.getId());
        assertThat(respuesta.vendible()).isTrue();
        assertThat(respuesta.motivo()).isNull();
        assertThat(respuesta.tipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(respuesta.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(respuesta.capacidad()).isEqualTo(45);
        assertThat(respuesta.sucursalOrigenId()).isEqualTo(XELA);
        assertThat(respuesta.fechaSalidaProgramada()).isAtSameInstantAs(SALIDA);
    }

    // R3: viajesActivos 
    @Test
    void viajesActivosDeUnBus() {
        when(repositorio.findByBusIdAndEstadoInOrderByFechaSalidaProgramadaAsc(viaje.getBusId(), EstadoViaje.ACTIVOS))
                .thenReturn(List.of(viaje));

        ViajesActivosRespuesta respuesta = servicio.viajesActivos(viaje.getBusId(), null);

        assertThat(respuesta.cantidad()).isEqualTo(1);
        assertThat(respuesta.viajes()).hasSize(1);
        assertThat(respuesta.viajes().get(0).viajeId()).isEqualTo(viaje.getId());
        assertThat(respuesta.viajes().get(0).estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verify(repositorio, never()).findByChoferIdAndEstadoInOrderByFechaSalidaProgramadaAsc(any(), any());
    }

    @Test
    void viajesActivosDeUnChofer() {
        when(repositorio.findByChoferIdAndEstadoInOrderByFechaSalidaProgramadaAsc(viaje.getChoferId(),
                EstadoViaje.ACTIVOS)).thenReturn(List.of());

        ViajesActivosRespuesta respuesta = servicio.viajesActivos(null, viaje.getChoferId());

        assertThat(respuesta.cantidad()).isZero();
        assertThat(respuesta.viajes()).isEmpty();
        verify(repositorio).findByChoferIdAndEstadoInOrderByFechaSalidaProgramadaAsc(viaje.getChoferId(),
                EstadoViaje.ACTIVOS);
        verify(repositorio, never()).findByBusIdAndEstadoInOrderByFechaSalidaProgramadaAsc(any(), any());
    }

}
