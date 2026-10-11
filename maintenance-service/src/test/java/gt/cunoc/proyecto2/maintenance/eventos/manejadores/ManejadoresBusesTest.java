package gt.cunoc.proyecto2.maintenance.eventos.manejadores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.maintenance.eventos.TipoEvento;
import gt.cunoc.proyecto2.maintenance.eventos.manejadores.fleet.ManejadorBusActualizado;
import gt.cunoc.proyecto2.maintenance.eventos.manejadores.fleet.ManejadorBusCreado;
import gt.cunoc.proyecto2.maintenance.eventos.manejadores.fleet.ManejadorBusDesactivado;
import gt.cunoc.proyecto2.maintenance.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.maintenance.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.maintenance.servicios.buses.ProyeccionBuses;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ManejadoresBusesTest {

    private final ProyeccionBuses proyeccion = Mockito.mock(ProyeccionBuses.class);

    private static <T> EventoRecibido<T> sobre(String tipo, T payload, Long version) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, UUID.randomUUID(), version,
                Instant.now(), LocalDate.of(2026, 10, 20), UUID.randomUUID(), payload);
    }

    private static void verifica(ManejadorEvento<?> manejador, TipoEvento tipo) {
        assertThat(manejador.tipoEvento()).isEqualTo(tipo.nombre());
        assertThat(manejador.tipoPayload()).isNotNull();
    }

    @Test
    void busCreadoYActualizadoGuardan() {
        var payload = new BusEvento(UUID.randomUUID(), UUID.randomUUID(), "C123ABC", "ACTIVO");
        var creado = new ManejadorBusCreado(proyeccion);
        verifica(creado, TipoEvento.BUS_CREADO);
        creado.manejar(sobre("BusCreado", payload, 0L));
        verify(proyeccion).guardar(payload.busId(), payload.sucursalId(), "C123ABC", "ACTIVO", 0L);

        var actualizado = new ManejadorBusActualizado(proyeccion);
        verifica(actualizado, TipoEvento.BUS_ACTUALIZADO);
        actualizado.manejar(sobre("BusActualizado", payload, 1L));
        verify(proyeccion).guardar(payload.busId(), payload.sucursalId(), "C123ABC", "ACTIVO", 1L);
    }

    @Test
    void busDesactivadoDesactiva() {
        var desactivado = new ManejadorBusDesactivado(proyeccion);
        verifica(desactivado, TipoEvento.BUS_DESACTIVADO);
        UUID busId = UUID.randomUUID();
        desactivado.manejar(sobre("BusDesactivado", new BusDesactivado(busId), 2L));
        verify(proyeccion).desactivar(busId, 2L);
        desactivado.manejar(sobre("BusDesactivado", new BusDesactivado(null), 2L));
    }
}
