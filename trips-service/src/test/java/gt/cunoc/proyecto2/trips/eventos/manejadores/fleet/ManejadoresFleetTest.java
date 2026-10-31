package gt.cunoc.proyecto2.trips.eventos.manejadores.fleet;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.FOTO_BUS;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferDesactivado;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Los 8 manejadores de eventos de Fleet: nombre, payload y que pasan la aggregateVersion a la proyección. */
class ManejadoresFleetTest {

    private static final Long VERSION = 7L;

    private final ProyeccionServicio proyecciones = mock(ProyeccionServicio.class);

    private <T> EventoRecibido<T> evento(String tipo, UUID agregadoId, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, agregadoId, VERSION, Instant.now(), FECHA,
                UUID.randomUUID(), payload);
    }

    // ----- Sucursales -----

    @Test
    void sucursalCreadaGuardaLaSucursal() {
        ManejadorSucursalCreada manejador = new ManejadorSucursalCreada(proyecciones);
        SucursalEvento sucursal = new SucursalEvento(XELA, "Xela Central", new BigDecimal("14.834999"),
                new BigDecimal("-91.518639"), true);

        manejador.manejar(evento("SucursalCreada", XELA, sucursal));

        assertThat(manejador.tipoEvento()).isEqualTo("SucursalCreada");
        assertThat(manejador.tipoPayload()).isEqualTo(SucursalEvento.class);
        verify(proyecciones).guardarSucursal(sucursal, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void sucursalActualizadaGuardaLaSucursal() {
        ManejadorSucursalActualizada manejador = new ManejadorSucursalActualizada(proyecciones);
        SucursalEvento sucursal = new SucursalEvento(XELA, "Xela", BigDecimal.ONE, BigDecimal.TEN, false);

        manejador.manejar(evento("SucursalActualizada", XELA, sucursal));

        assertThat(manejador.tipoEvento()).isEqualTo("SucursalActualizada");
        assertThat(manejador.tipoPayload()).isEqualTo(SucursalEvento.class);
        verify(proyecciones).guardarSucursal(sucursal, VERSION);
    }

    // ----- Buses -----

    @Test
    void busCreadoGuardaElBus() {
        ManejadorBusCreado manejador = new ManejadorBusCreado(proyecciones);
        BusEvento bus = new BusEvento(UUID.randomUUID(), XELA, "P-123ABC", 45, "ACTIVO", new BigDecimal("12500.00"), FOTO_BUS);

        manejador.manejar(evento("BusCreado", bus.busId(), bus));

        assertThat(manejador.tipoEvento()).isEqualTo("BusCreado");
        assertThat(manejador.tipoPayload()).isEqualTo(BusEvento.class);
        verify(proyecciones).guardarBus(bus, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void busActualizadoGuardaElBus() {
        ManejadorBusActualizado manejador = new ManejadorBusActualizado(proyecciones);
        BusEvento bus = new BusEvento(UUID.randomUUID(), XELA, "P-123ABC", 50, "EN_TALLER", new BigDecimal("12500.00"), FOTO_BUS);

        manejador.manejar(evento("BusActualizado", bus.busId(), bus));

        assertThat(manejador.tipoEvento()).isEqualTo("BusActualizado");
        assertThat(manejador.tipoPayload()).isEqualTo(BusEvento.class);
        verify(proyecciones).guardarBus(bus, VERSION);
    }

    @Test
    void busDesactivadoDesactivaElBus() {
        ManejadorBusDesactivado manejador = new ManejadorBusDesactivado(proyecciones);
        UUID busId = UUID.randomUUID();

        manejador.manejar(evento("BusDesactivado", busId, new BusDesactivado(busId)));

        assertThat(manejador.tipoEvento()).isEqualTo("BusDesactivado");
        assertThat(manejador.tipoPayload()).isEqualTo(BusDesactivado.class);
        verify(proyecciones).desactivarBus(busId, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    // ----- Choferes -----

    @Test
    void choferCreadoGuardaElChofer() {
        ManejadorChoferCreado manejador = new ManejadorChoferCreado(proyecciones);
        ChoferEvento chofer = new ChoferEvento(UUID.randomUUID(), UUID.randomUUID(), XELA, "Juan Perez", new BigDecimal("350.00"),
                "ACTIVO");

        manejador.manejar(evento("ChoferCreado", chofer.choferId(), chofer));

        assertThat(manejador.tipoEvento()).isEqualTo("ChoferCreado");
        assertThat(manejador.tipoPayload()).isEqualTo(ChoferEvento.class);
        verify(proyecciones).guardarChofer(chofer, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void choferActualizadoGuardaElChofer() {
        ManejadorChoferActualizado manejador = new ManejadorChoferActualizado(proyecciones);
        ChoferEvento chofer = new ChoferEvento(UUID.randomUUID(), UUID.randomUUID(), XELA, "Juan Pérez", new BigDecimal("400.00"),
                "ACTIVO");

        manejador.manejar(evento("ChoferActualizado", chofer.choferId(), chofer));

        assertThat(manejador.tipoEvento()).isEqualTo("ChoferActualizado");
        assertThat(manejador.tipoPayload()).isEqualTo(ChoferEvento.class);
        verify(proyecciones).guardarChofer(chofer, VERSION);
    }

    @Test
    void choferDesactivadoDesactivaElChofer() {
        ManejadorChoferDesactivado manejador = new ManejadorChoferDesactivado(proyecciones);
        UUID choferId = UUID.randomUUID();

        manejador.manejar(evento("ChoferDesactivado", choferId, new ChoferDesactivado(choferId)));

        assertThat(manejador.tipoEvento()).isEqualTo("ChoferDesactivado");
        assertThat(manejador.tipoPayload()).isEqualTo(ChoferDesactivado.class);
        verify(proyecciones).desactivarChofer(choferId, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void sinAggregateVersionPasaNulo() {
        ManejadorBusCreado manejador = new ManejadorBusCreado(proyecciones);
        BusEvento bus = new BusEvento(UUID.randomUUID(), XELA, "P-999XYZ", 40, "ACTIVO", new BigDecimal("12500.00"), FOTO_BUS);

        manejador.manejar(new EventoRecibido<>(UUID.randomUUID(), "BusCreado", 1, bus.busId(), null,
                Instant.now(), FECHA, UUID.randomUUID(), bus));

        verify(proyecciones).guardarBus(bus, null);
    }
}
