package gt.cunoc.proyecto2.fleet.modelos.buses;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.buses.ErrorBus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusEntidadTest {

    private final UUID sucursalId = UUID.randomUUID();

    @Test
    void unBusNuevoQuedaActivoYSinVersion() {
        BusEntidad bus = BusEntidad.registrar(sucursalId, "P-123ABC", "Volvo", "B12", 2018, 50,
                new BigDecimal("1000.00"), DatosPrueba.FOTO_BUS);

        assertThat(bus.getId()).isNotNull();
        assertThat(bus.getSucursalId()).isEqualTo(sucursalId);
        assertThat(bus.getPlaca()).isEqualTo("P-123ABC");
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.ACTIVO);
        assertThat(bus.getKilometrajeActual()).isEqualByComparingTo("1000.00");
        assertThat(bus.getVersion()).isNull();
    }

    @Test
    void enRutaNoSeAsignaAMano() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);

        VerificarError.lanza(() -> bus.cambiarEstado(EstadoBus.EN_RUTA), ErrorBus.ESTADO_NO_PERMITIDO);
    }

    @Test
    void unBusEnRutaNoCambiaDeEstadoAMano() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);
        bus.registrarSalida();

        VerificarError.lanza(() -> bus.cambiarEstado(EstadoBus.EN_TALLER), ErrorBus.BUS_EN_RUTA);
    }

    @Test
    void cambioManualDeEstado() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);

        bus.cambiarEstado(EstadoBus.EN_TALLER);

        assertThat(bus.getEstado()).isEqualTo(EstadoBus.EN_TALLER);
    }

    @Test
    void laSalidaSoloAplicaAUnBusActivo() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);

        assertThat(bus.registrarSalida()).isTrue();
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.EN_RUTA);
        assertThat(bus.registrarSalida()).isFalse();          // evento repetido

        BusEntidad enTaller = DatosPrueba.bus(sucursalId);
        enTaller.cambiarEstado(EstadoBus.EN_TALLER);
        assertThat(enTaller.registrarSalida()).isFalse();
        assertThat(enTaller.getEstado()).isEqualTo(EstadoBus.EN_TALLER);
    }

    @Test
    void laLlegadaSubeElKilometrajeYRegresaAActivo() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);
        bus.registrarSalida();

        bus.registrarLlegada(new BigDecimal("1250.50"));

        assertThat(bus.getKilometrajeActual()).isEqualByComparingTo("1250.50");
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.ACTIVO);
    }

    @Test
    void unaLlegadaViejaNoBajaElKilometrajeNiCambiaOtroEstado() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);
        bus.cambiarEstado(EstadoBus.EN_TALLER);

        bus.registrarLlegada(new BigDecimal("900.00"));

        assertThat(bus.getKilometrajeActual()).isEqualByComparingTo("1000.00");
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.EN_TALLER);
    }
}
