package gt.cunoc.proyecto2.trips.servicios.operaciones;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.LLEGADA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarLlegadaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarSalidaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.buses.VistaBusRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.LlegadaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.SalidaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OperacionViajeServicioImplTest {

    /** Día de LLEGADA en Guatemala. */
    private static final LocalDate DIA_LLEGADA = LocalDate.of(2026, 10, 25);

    private final ViajeServicio viajes = mock(ViajeServicio.class);
    private final ViajeRepositorio viajeRepositorio = mock(ViajeRepositorio.class);
    private final SalidaViajeRepositorio salidas = mock(SalidaViajeRepositorio.class);
    private final LlegadaViajeRepositorio llegadas = mock(LlegadaViajeRepositorio.class);
    private final VistaTarifaDepreciacionRepositorio tarifas = mock(VistaTarifaDepreciacionRepositorio.class);
    private final AlquilerPrivadoRepositorio alquileres = mock(AlquilerPrivadoRepositorio.class);
    private final VistaBusRepositorio buses = mock(VistaBusRepositorio.class);
    private final PublicadorEventosTrips eventos = mock(PublicadorEventosTrips.class);

    private final OperacionViajeServicioImpl servicio = new OperacionViajeServicioImpl(viajes, viajeRepositorio,
            salidas, llegadas, tarifas, alquileres, buses, eventos);

    /** Viaje REGULAR PROGRAMADO de Xela. */
    private final ViajeEntidad viaje = DatosPrueba.viajeRegular();
    /** Lo que devuelve ViajeServicio.detalle (aquí solo importa que se devuelva tal cual). */
    private final ViajeDetalleRespuesta detalle = new ViajeDetalleRespuesta(null, null, null);

    private void prepararViaje(ViajeEntidad existente) {
        when(viajes.obtenerPorId(existente.getId())).thenReturn(existente);
        when(viajes.detalle(existente)).thenReturn(detalle);
        when(viajeRepositorio.saveAndFlush(any(ViajeEntidad.class))).thenAnswer(i -> i.getArgument(0));
        when(salidas.saveAndFlush(any(SalidaViajeEntidad.class))).thenAnswer(i -> i.getArgument(0));
        when(llegadas.saveAndFlush(any(LlegadaViajeEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    /** El viaje ya salió (EN_RUTA) y su salida (06:05, 1000 km) está guardada. */
    private SalidaViajeEntidad yaSalio(ViajeEntidad enRuta) {
        enRuta.iniciar();
        SalidaViajeEntidad salida = DatosPrueba.salida(enRuta);
        when(salidas.findById(enRuta.getId())).thenReturn(Optional.of(salida));
        return salida;
    }

    private void hayTarifa(LocalDate dia, String montoPorKm) {
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(dia))
                .thenReturn(Optional.of(DatosPrueba.vistaTarifa(montoPorKm, LocalDate.of(2026, 1, 1))));
    }

    private static RegistrarSalidaSolicitud salidaSolicitud() {
        return new RegistrarSalidaSolicitud(SALIDA.plusMinutes(5), new BigDecimal("1000.00"));
    }

    /** Llegada a las 10:40 con 1207.50 km y Q950 de combustible. */
    private static RegistrarLlegadaSolicitud llegadaSolicitud() {
        return new RegistrarLlegadaSolicitud(LLEGADA.plusMinutes(10), new BigDecimal("1207.50"),
                new BigDecimal("950.00"));
    }

    // ----- T14: registrarSalida -----

    @Test
    void elChoferAsignadoRegistraLaSalida() {
        prepararViaje(viaje);
        UsuarioActual chofer = DatosPrueba.chofer(viaje.getChoferId());

        ViajeDetalleRespuesta respuesta = servicio.registrarSalida(viaje.getId(), salidaSolicitud(), chofer);

        assertThat(respuesta).isSameAs(detalle);
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.EN_RUTA);
        ArgumentCaptor<SalidaViajeEntidad> guardada = ArgumentCaptor.forClass(SalidaViajeEntidad.class);
        verify(salidas).saveAndFlush(guardada.capture());
        SalidaViajeEntidad salida = guardada.getValue();
        assertThat(salida.getViajeId()).isEqualTo(viaje.getId());
        assertThat(salida.getBusId()).isEqualTo(viaje.getBusId());
        assertThat(salida.getChoferId()).isEqualTo(viaje.getChoferId());
        assertThat(salida.getKmInicial()).isEqualByComparingTo("1000.00");
        assertThat(salida.getFechaHoraSalida()).isAtSameInstantAs(SALIDA.plusMinutes(5));
        assertThat(salida.getRegistradoPor()).isEqualTo(chofer.usuarioId());
        verify(viajeRepositorio).saveAndFlush(viaje);
        verify(eventos).viajeIniciado(viaje, salida);
    }

    @Test
    void elAdminDeLaSucursalRegistraLaSalida() {
        prepararViaje(viaje);
        UsuarioActual adminXela = DatosPrueba.adminSucursal(XELA);

        servicio.registrarSalida(viaje.getId(), salidaSolicitud(), adminXela);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.EN_RUTA);
        ArgumentCaptor<SalidaViajeEntidad> publicada = ArgumentCaptor.forClass(SalidaViajeEntidad.class);
        verify(eventos).viajeIniciado(eq(viaje), publicada.capture());
        assertThat(publicada.getValue().getRegistradoPor()).isEqualTo(adminXela.usuarioId());
    }

    @Test
    void elAdminDeOtraSucursalNoRegistraLaSalida() {
        prepararViaje(viaje);

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.adminSucursal(GUATE)), "SUCURSAL_AJENA");
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verifyNoInteractions(salidas, eventos);
    }

    @Test
    void otroUsuarioNoRegistraLaSalida() {
        prepararViaje(viaje);

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(UUID.randomUUID())), ErrorOperacionViaje.CHOFER_NO_ASIGNADO);
        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.cajero(XELA)), ErrorOperacionViaje.CHOFER_NO_ASIGNADO);
        // ADMIN_SISTEMA no es el chofer ni ADMIN_SUCURSAL
        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.adminSistema()), ErrorOperacionViaje.CHOFER_NO_ASIGNADO);
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verifyNoInteractions(salidas, eventos);
    }

    /** Un bus no arranca dos viajes a la vez: la vuelta espera a que la ida registre su llegada. */
    @Test
    void siElBusVieneEnRutaEnOtroViajeLaSalidaSeRechaza() {
        prepararViaje(viaje);
        when(viajeRepositorio.existsByBusIdAndEstadoAndIdNot(viaje.getBusId(), EstadoViaje.EN_RUTA, viaje.getId()))
                .thenReturn(true);

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.BUS_EN_RUTA);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verifyNoInteractions(salidas);
    }

    @Test
    void siElPilotoVieneEnRutaEnOtroViajeLaSalidaSeRechaza() {
        prepararViaje(viaje);
        when(viajeRepositorio.existsByChoferIdAndEstadoAndIdNot(viaje.getChoferId(), EstadoViaje.EN_RUTA,
                viaje.getId())).thenReturn(true);

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.CHOFER_EN_RUTA);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
    }

    /** Los viajes se arrancan en orden: el que sale antes va primero. */
    @Test
    void siHayOtroViajeQueSaleAntesLaSalidaSeRechaza() {
        prepararViaje(viaje);
        when(viajeRepositorio.existeOtroAntesDeEste(viaje.getChoferId(), viaje.getBusId(), viaje.getId(),
                EstadoViaje.PROGRAMADO, viaje.getFechaSalidaProgramada())).thenReturn(true);

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.HAY_UN_VIAJE_ANTERIOR);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verifyNoInteractions(salidas);
    }

    /** El regreso espera a que su ida esté COMPLETADA; si la ida ya no existe, opera por su cuenta. */
    @Test
    void elRegresoNoSaleMientrasSuIdaNoEsteCompletada() {
        ViajeEntidad ida = DatosPrueba.viajeRegular();
        ViajeEntidad vuelta = DatosPrueba.viajeDeRegreso(ida);
        prepararViaje(vuelta);
        when(viajeRepositorio.findById(ida.getId())).thenReturn(Optional.of(ida));

        VerificarError.lanza(() -> servicio.registrarSalida(vuelta.getId(), salidaSolicitud(),
                DatosPrueba.chofer(vuelta.getChoferId())), ErrorOperacionViaje.IDA_SIN_COMPLETAR);

        assertThat(vuelta.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verifyNoInteractions(salidas);
    }

    @Test
    void conLaIdaCompletadaElRegresoYaPuedeSalir() {
        ViajeEntidad ida = DatosPrueba.viajeRegular();
        ida.iniciar();
        ida.completar();
        ViajeEntidad vuelta = DatosPrueba.viajeDeRegreso(ida);
        prepararViaje(vuelta);
        when(viajeRepositorio.findById(ida.getId())).thenReturn(Optional.of(ida));

        // La vuelta sale a su propia hora, no a la de la ida: salir antes de lo programado se rechaza.
        RegistrarSalidaSolicitud aSuHora = new RegistrarSalidaSolicitud(vuelta.getFechaSalidaProgramada(),
                new BigDecimal("1000.00"));

        servicio.registrarSalida(vuelta.getId(), aSuHora, DatosPrueba.chofer(vuelta.getChoferId()));

        assertThat(vuelta.getEstado()).isEqualTo(EstadoViaje.EN_RUTA);
    }

    /** El odómetro no camina para atrás: la salida no se registra con menos km de los que trae el bus. */
    @Test
    void elKilometrajeInicialNoPuedeSerMenorAlDelBus() {
        prepararViaje(viaje);
        when(buses.findById(viaje.getBusId()))
                .thenReturn(Optional.of(DatosPrueba.vistaBusCon(viaje.getBusId(), "1500.00")));

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.KM_MENOR_AL_DEL_BUS);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
    }

    /** Aunque Fleet venga atrasado, la última salida registrada aquí también cuenta. */
    @Test
    void elKilometrajeTambienSeComparaConLaUltimaSalidaRegistrada() {
        prepararViaje(viaje);
        when(salidas.ultimoKilometrajeDelBus(viaje.getBusId())).thenReturn(new BigDecimal("2000.00"));

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.KM_MENOR_AL_DEL_BUS);
    }

    /** La salida puede atrasarse, pero no adelantarse a la hora a la que el viaje estaba programado. */
    @Test
    void laSalidaNoPuedeSerAntesDeLaHoraProgramada() {
        prepararViaje(viaje);
        RegistrarSalidaSolicitud adelantada = new RegistrarSalidaSolicitud(SALIDA.minusMinutes(1),
                new BigDecimal("1000.00"));

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), adelantada,
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.SALIDA_ANTES_DE_LO_PROGRAMADO);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        verifyNoInteractions(eventos);
    }

    @Test
    void laSalidaNoSeRegistraDosVeces() {
        prepararViaje(viaje);
        viaje.iniciar();

        VerificarError.lanza(() -> servicio.registrarSalida(viaje.getId(), salidaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.SALIDA_YA_REGISTRADA);
        verify(salidas, never()).saveAndFlush(any(SalidaViajeEntidad.class));
        verify(viajeRepositorio, never()).saveAndFlush(any(ViajeEntidad.class));
        verifyNoInteractions(eventos);
    }

    // ----- T15: registrarLlegada -----

    @Test
    void laLlegadaSinSalidaFalla() {
        prepararViaje(viaje);

        VerificarError.lanza(() -> servicio.registrarLlegada(viaje.getId(), llegadaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.VIAJE_NO_INICIADO);
        verifyNoInteractions(llegadas, tarifas, eventos);
    }

    @Test
    void laLlegadaFallaSiNoEncuentraLaFilaDeSalida() {
        prepararViaje(viaje);
        viaje.iniciar();
        when(salidas.findById(viaje.getId())).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.registrarLlegada(viaje.getId(), llegadaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.VIAJE_NO_INICIADO);
        verifyNoInteractions(llegadas, eventos);
    }

    @Test
    void laLlegadaNoSeRegistraDosVeces() {
        prepararViaje(viaje);
        yaSalio(viaje);
        viaje.completar();

        VerificarError.lanza(() -> servicio.registrarLlegada(viaje.getId(), llegadaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.LLEGADA_YA_REGISTRADA);
        verifyNoInteractions(llegadas, eventos);
    }

    @Test
    void laLlegadaDeOtroUsuarioEstaProhibida() {
        prepararViaje(viaje);
        yaSalio(viaje);

        VerificarError.lanza(() -> servicio.registrarLlegada(viaje.getId(), llegadaSolicitud(),
                DatosPrueba.cliente()), ErrorOperacionViaje.CHOFER_NO_ASIGNADO);
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.EN_RUTA);
        verifyNoInteractions(llegadas, eventos);
    }

    @Test
    void laLlegadaSinTarifaVigente() {
        prepararViaje(viaje);
        yaSalio(viaje);
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(DIA_LLEGADA))
                .thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.registrarLlegada(viaje.getId(), llegadaSolicitud(),
                DatosPrueba.chofer(viaje.getChoferId())), ErrorOperacionViaje.SIN_TARIFA_VIGENTE);
        verify(llegadas, never()).saveAndFlush(any(LlegadaViajeEntidad.class));
        verifyNoInteractions(eventos);
    }
}
