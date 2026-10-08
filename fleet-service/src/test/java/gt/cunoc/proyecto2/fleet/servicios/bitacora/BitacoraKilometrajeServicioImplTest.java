package gt.cunoc.proyecto2.fleet.servicios.bitacora;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.bitacora.ErrorBitacora;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.repositorios.bitacora.BitacoraKilometrajeRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.buses.BusServicio;
import gt.cunoc.proyecto2.fleet.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class BitacoraKilometrajeServicioImplTest {

    private static final OffsetDateTime LLEGADA = OffsetDateTime.parse("2026-10-20T18:00:00-06:00");

    @Mock
    private BitacoraKilometrajeRepositorio repositorio;
    @Mock
    private BusServicio buses;
    @Mock
    private BusRepositorio busRepositorio;
    @Mock
    private PublicadorEventosFleet eventos;

    private BitacoraKilometrajeServicioImpl servicio;
    private BusEntidad bus;
    private final UUID viajeId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        servicio = new BitacoraKilometrajeServicioImpl(repositorio, buses, busRepositorio, eventos);
        bus = DatosPrueba.bus(UUID.randomUUID());
    }

    private void elBusExiste() {
        when(busRepositorio.findById(bus.getId())).thenReturn(Optional.of(bus));
    }

    private void guardarDevuelveElMismo() {
        when(busRepositorio.saveAndFlush(any(BusEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ----- F11 y F23 -----

    @Test
    void ultimosCincoViajes() {
        BitacoraKilometrajeEntidad registro = BitacoraKilometrajeEntidad.registrar(bus.getId(), viajeId,
                BigDecimal.ZERO, BigDecimal.TEN, LLEGADA);
        when(repositorio.findTop5ByBusIdOrderByFechaLlegadaDesc(bus.getId())).thenReturn(List.of(registro));

        assertThat(servicio.ultimosViajes(bus.getId())).containsExactly(registro);
    }

    @Test
    void rangoDeFechasAlReves() {
        UsuarioActual admin = DatosPrueba.adminSistema();

        VerificarError.lanza(() -> servicio.listar(bus.getId(), FECHA, FECHA.minusDays(1), admin),
                ErrorBitacora.RANGO_FECHAS_INVALIDO);
        verifyNoInteractions(buses, repositorio);
    }

    @Test
    void listarVerificaElAccesoYFiltraPorDiaEnGuatemala() {
        UsuarioActual admin = DatosPrueba.adminSistema();
        LocalDate desde = FECHA.minusDays(7);
        when(repositorio.buscarPorRango(bus.getId(), Fechas.inicioDelDia(desde), Fechas.finDelDia(FECHA)))
                .thenReturn(List.of());

        assertThat(servicio.listar(bus.getId(), desde, FECHA, admin)).isEmpty();
        verify(buses).obtener(bus.getId(), admin);
    }

    @Test
    void listarSinFechasUsaLosLimitesAmplios() {
        UsuarioActual admin = DatosPrueba.adminSistema();
        when(repositorio.buscarPorRango(bus.getId(), Fechas.inicioDelDia(null), Fechas.finDelDia(null)))
                .thenReturn(List.of());

        assertThat(servicio.listar(bus.getId(), null, null, admin)).isEmpty();
    }

    // ----- Evento ViajeIniciado -----

    @Test
    void viajeIniciadoPoneElBusEnRutaYPublica() {
        elBusExiste();
        guardarDevuelveElMismo();

        assertThat(servicio.registrarViajeIniciado(bus.getId(), viajeId, FECHA)).isTrue();

        assertThat(bus.getEstado()).isEqualTo(EstadoBus.EN_RUTA);
        verify(eventos).busActualizado(bus, FECHA);
    }

    @Test
    void viajeIniciadoTardioDeUnViajeYaCompletadoSeIgnora() {
        when(repositorio.existsByViajeId(viajeId)).thenReturn(true);

        assertThat(servicio.registrarViajeIniciado(bus.getId(), viajeId, FECHA)).isFalse();
        verifyNoInteractions(busRepositorio, eventos);
    }

    @Test
    void viajeIniciadoDeUnBusQueNoExiste() {
        assertThat(servicio.registrarViajeIniciado(bus.getId(), viajeId, FECHA)).isFalse();
        verifyNoInteractions(eventos);
    }

    @Test
    void viajeIniciadoDeUnBusEnTallerNoLoCambia() {
        bus.cambiarEstado(EstadoBus.EN_TALLER);
        elBusExiste();

        assertThat(servicio.registrarViajeIniciado(bus.getId(), viajeId, FECHA)).isFalse();
        verify(busRepositorio, never()).saveAndFlush(any(BusEntidad.class));
        verifyNoInteractions(eventos);
    }

    // ----- Evento ViajeCompletado -----

    @Test
    void viajeCompletadoRegistraLaBitacoraSubeElKilometrajeYPublica() {
        bus.registrarSalida();
        elBusExiste();
        guardarDevuelveElMismo();

        boolean registrado = servicio.registrarViajeCompletado(bus.getId(), viajeId, new BigDecimal("1000.00"),
                new BigDecimal("1180.00"), LLEGADA, FECHA);

        assertThat(registrado).isTrue();
        ArgumentCaptor<BitacoraKilometrajeEntidad> bitacora = ArgumentCaptor.forClass(BitacoraKilometrajeEntidad.class);
        verify(repositorio).save(bitacora.capture());
        assertThat(bitacora.getValue().getViajeId()).isEqualTo(viajeId);
        assertThat(bitacora.getValue().kmRecorridos()).isEqualByComparingTo("180.00");
        assertThat(bus.getKilometrajeActual()).isEqualByComparingTo("1180.00");
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.ACTIVO);
        verify(eventos).busActualizado(bus, FECHA);
    }


}
