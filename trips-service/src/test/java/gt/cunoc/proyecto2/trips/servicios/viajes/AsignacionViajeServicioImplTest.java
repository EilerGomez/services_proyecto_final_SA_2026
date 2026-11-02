package gt.cunoc.proyecto2.trips.servicios.viajes;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.LLEGADA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.clientes.fleet.ClienteFleet;
import gt.cunoc.proyecto2.trips.dtos.fleet.ChoferFleetRespuesta;
import gt.cunoc.proyecto2.trips.dtos.fleet.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AsignacionViajeServicioImplTest {

    /** El mismo valor que usa el servicio para un viaje que aún no tiene id. */
    private static final UUID SIN_VIAJE = new UUID(0L, 0L);
    /** Día de SALIDA en Guatemala. */
    private static final LocalDate DIA_SALIDA = LocalDate.of(2026, 10, 25);
    private static final BigDecimal SALARIO_NUEVO = new BigDecimal("400.00");

    private final ClienteFleet fleet = mock(ClienteFleet.class);
    private final ViajeRepositorio viajes = mock(ViajeRepositorio.class);

    private final AsignacionViajeServicioImpl servicio = new AsignacionViajeServicioImpl(fleet, viajes);

    private final UUID busId = UUID.randomUUID();
    private final UUID choferId = UUID.randomUUID();

    // ----- Respuestas de Fleet -----

    private static DisponibilidadBusRespuesta bus(UUID id, UUID sucursalId, int capacidad, String estado,
                                                  boolean disponible) {
        return new DisponibilidadBusRespuesta(id, sucursalId, "P-123ABC", capacidad, estado, disponible,
                disponible ? null : "BUS_" + estado);
    }

    private static ChoferFleetRespuesta chofer(UUID id, UUID sucursalId, String estado, boolean licenciaVigente,
                                               BigDecimal salario) {
        return new ChoferFleetRespuesta(id, sucursalId, UUID.randomUUID(), "Juan Pérez", estado, salario,
                LocalDate.of(2026, 10, 1), licenciaVigente);
    }

    private void fleetTieneElBus(UUID id, UUID sucursalId, int capacidad) {
        when(fleet.disponibilidadDelBus(id)).thenReturn(Optional.of(bus(id, sucursalId, capacidad, "ACTIVO", true)));
    }

    private void fleetTieneElChofer(UUID id, LocalDate dia, BigDecimal salario) {
        when(fleet.choferParaViaje(id, dia)).thenReturn(Optional.of(chofer(id, XELA, "ACTIVO", true, salario)));
    }

    /** El código del error y una parte del mensaje (para distinguir las variantes de un mismo código). */
    private static void lanza(ThrowingCallable accion, CodigoError error, String parteDelMensaje) {
        VerificarError.lanza(accion, error);
        assertThatThrownBy(accion).hasMessageContaining(parteDelMensaje);
    }

    private AsignacionViaje nueva() {
        return servicio.validarNueva(XELA, busId, choferId, SALIDA, LLEGADA);
    }

    // ----- validarNueva -----

    @Test
    void nuevaConFechasInvalidasNoLlamaAFleet() {
        VerificarError.lanza(() -> servicio.validarNueva(XELA, busId, choferId, SALIDA, SALIDA),
                ErrorViaje.FECHAS_INVALIDAS);
        VerificarError.lanza(() -> servicio.validarNueva(XELA, busId, choferId, LLEGADA, SALIDA),
                ErrorViaje.FECHAS_INVALIDAS);
        verifyNoInteractions(fleet, viajes);
    }

    @Test
    void nuevaConUnBusQueNoExisteEnFleet() {
        when(fleet.disponibilidadDelBus(busId)).thenReturn(Optional.empty());

        lanza(this::nueva, ErrorViaje.BUS_NO_DISPONIBLE, "no existe en Flota");
        verify(fleet, never()).choferParaViaje(any(), any());
        verifyNoInteractions(viajes);
    }

    @Test
    void nuevaConUnBusDeOtraSucursal() {
        fleetTieneElBus(busId, GUATE, 45);

        lanza(this::nueva, ErrorViaje.BUS_NO_DISPONIBLE, "es de otra sucursal");
        verify(fleet, never()).choferParaViaje(any(), any());
    }

    @Test
    void nuevaConUnBusNoDisponible() {
        when(fleet.disponibilidadDelBus(busId))
                .thenReturn(Optional.of(bus(busId, XELA, 45, "EN_TALLER", false)));

        lanza(this::nueva, ErrorViaje.BUS_NO_DISPONIBLE, "está EN_TALLER");
        verify(fleet, never()).choferParaViaje(any(), any());
    }

    @Test
    void nuevaConUnChoferQueNoExisteEnFleet() {
        fleetTieneElBus(busId, XELA, 45);
        when(fleet.choferParaViaje(choferId, DIA_SALIDA)).thenReturn(Optional.empty());

        lanza(this::nueva, ErrorViaje.CHOFER_NO_DISPONIBLE, "no existe en Flota");
        verifyNoInteractions(viajes);
    }

    @Test
    void nuevaConUnChoferDeOtraSucursal() {
        fleetTieneElBus(busId, XELA, 45);
        when(fleet.choferParaViaje(choferId, DIA_SALIDA))
                .thenReturn(Optional.of(chofer(choferId, GUATE, "ACTIVO", true, SALARIO_NUEVO)));

        lanza(this::nueva, ErrorViaje.CHOFER_NO_DISPONIBLE, "Juan Pérez es de otra sucursal");
    }

    @Test
    void nuevaConUnChoferInactivo() {
        fleetTieneElBus(busId, XELA, 45);
        when(fleet.choferParaViaje(choferId, DIA_SALIDA))
                .thenReturn(Optional.of(chofer(choferId, XELA, "INACTIVO", true, SALARIO_NUEVO)));

        lanza(this::nueva, ErrorViaje.CHOFER_NO_DISPONIBLE, "Juan Pérez está inactivo");
    }

    @Test
    void nuevaConLaLicenciaVencida() {
        fleetTieneElBus(busId, XELA, 45);
        when(fleet.choferParaViaje(choferId, DIA_SALIDA))
                .thenReturn(Optional.of(chofer(choferId, XELA, "ACTIVO", false, SALARIO_NUEVO)));

        lanza(this::nueva, ErrorViaje.LICENCIA_VENCIDA, "2026-10-01");
        verifyNoInteractions(viajes);
    }

    @Test
    void nuevaConElBusOcupado() {
        fleetTieneElBus(busId, XELA, 45);
        fleetTieneElChofer(choferId, DIA_SALIDA, SALARIO_NUEVO);
        when(viajes.existeChoqueDeBus(busId, SIN_VIAJE, EstadoViaje.ACTIVOS, SALIDA, LLEGADA)).thenReturn(true);

        VerificarError.lanza(this::nueva, ErrorViaje.BUS_OCUPADO);
        verify(viajes, never()).existeChoqueDeChofer(any(), any(), any(), any(), any());
    }

    @Test
    void nuevaConElChoferOcupado() {
        fleetTieneElBus(busId, XELA, 45);
        fleetTieneElChofer(choferId, DIA_SALIDA, SALARIO_NUEVO);
        when(viajes.existeChoqueDeChofer(choferId, SIN_VIAJE, EstadoViaje.ACTIVOS, SALIDA, LLEGADA))
                .thenReturn(true);

        VerificarError.lanza(this::nueva, ErrorViaje.CHOFER_OCUPADO);
    }

    // ----- validarCambio (reprogramar un viaje que ya existe) -----

    /** El viaje que se reprograma: bus y chofer propios, 45 asientos, salario 350.00, salida el 25. */
    private ViajeEntidad viajeExistente() {
        return DatosPrueba.viajeRegular(DatosPrueba.ruta(), DatosPrueba.asignacion(busId, choferId, 45));
    }

    private AsignacionViaje cambio(ViajeEntidad viaje, UUID nuevoBus, UUID nuevoChofer,
                                   OffsetDateTime salida, OffsetDateTime llegada) {
        return servicio.validarCambio(viaje, nuevoBus, nuevoChofer, salida, llegada);
    }

    @Test
    void cambiarSoloLaHoraDentroDelMismoDiaNoLePreguntaNadaAFleet() {
        // Arrange: mismo bus, mismo chofer y el mismo día; solo se corre el horario
        ViajeEntidad viaje = viajeExistente();

        // Act
        AsignacionViaje resultado = cambio(viaje, busId, choferId, SALIDA.plusHours(1), LLEGADA.plusHours(1));

        // Assert: conserva capacidad y salario del viaje, y Fleet ni se entera
        assertThat(resultado.capacidadBus()).isEqualTo(45);
        assertThat(resultado.salarioBaseChofer()).isEqualByComparingTo(DatosPrueba.SALARIO);
        assertThat(resultado.fechaSalidaProgramada()).isEqualTo(SALIDA.plusHours(1));
        verifyNoInteractions(fleet);
    }

    @Test
    void cambiarAUnBusConMenosAsientosSeRechazaAntesDePreguntarPorElChofer() {
        // Arrange: el bus nuevo trae 30 asientos y el viaje ya se vendía con 45
        ViajeEntidad viaje = viajeExistente();
        UUID otroBus = UUID.randomUUID();
        fleetTieneElBus(otroBus, XELA, 30);

        // Act + Assert
        lanza(() -> cambio(viaje, otroBus, choferId, SALIDA, LLEGADA), ErrorViaje.CAPACIDAD_MENOR, "30");
        verify(fleet, never()).choferParaViaje(any(), any());
        verifyNoInteractions(viajes);
    }

    @Test
    void cambiarDeChoferTomaElSalarioDelNuevo() {
        // Arrange: mismo bus, otro piloto
        ViajeEntidad viaje = viajeExistente();
        UUID otroChofer = UUID.randomUUID();
        fleetTieneElChofer(otroChofer, DIA_SALIDA, SALARIO_NUEVO);

        // Act
        AsignacionViaje resultado = cambio(viaje, busId, otroChofer, SALIDA, LLEGADA);

        // Assert: el salario es el del piloto nuevo y el bus no se revalidó
        assertThat(resultado.salarioBaseChofer()).isEqualByComparingTo(SALARIO_NUEVO);
        assertThat(resultado.capacidadBus()).isEqualTo(45);
        verify(fleet, never()).disponibilidadDelBus(any());
    }

    @Test
    void moverElViajeAOtroDiaRevalidaLaLicenciaDelMismoChoferSinCambiarleElSalario() {
        // Arrange: mismo bus y mismo piloto, pero el viaje se corre al día siguiente
        ViajeEntidad viaje = viajeExistente();
        OffsetDateTime salida = SALIDA.plusDays(1);
        fleetTieneElChofer(choferId, DIA_SALIDA.plusDays(1), SALARIO_NUEVO);

        // Act
        AsignacionViaje resultado = cambio(viaje, busId, choferId, salida, LLEGADA.plusDays(1));

        // Assert: se preguntó por la licencia de ese día, pero el salario sigue siendo el del viaje
        verify(fleet).choferParaViaje(choferId, DIA_SALIDA.plusDays(1));
        assertThat(resultado.salarioBaseChofer()).isEqualByComparingTo(DatosPrueba.SALARIO);
    }

    @Test
    void alReprogramarElChoqueDeHorarioNoCuentaElViajeQueSeEstaCambiando() {
        // Arrange: el repositorio recibe el id del propio viaje para excluirlo, y aun así hay choque
        ViajeEntidad viaje = viajeExistente();
        UUID viajeId = UUID.randomUUID();
        ReflectionTestUtils.setField(viaje, "id", viajeId);
        when(viajes.existeChoqueDeBus(busId, viajeId, EstadoViaje.ACTIVOS, SALIDA, LLEGADA))
                .thenReturn(true);

        // Act + Assert
        VerificarError.lanza(() -> cambio(viaje, busId, choferId, SALIDA, LLEGADA), ErrorViaje.BUS_OCUPADO);
        verify(viajes, never()).existeChoqueDeChofer(any(), any(), any(), any(), any());
    }
}
