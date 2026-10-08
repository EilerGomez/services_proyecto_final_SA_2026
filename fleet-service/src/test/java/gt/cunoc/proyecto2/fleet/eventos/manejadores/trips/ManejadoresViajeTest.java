package gt.cunoc.proyecto2.fleet.eventos.manejadores.trips;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.fleet.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.fleet.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.fleet.servicios.bitacora.BitacoraKilometrajeServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManejadoresViajeTest {

    private final BitacoraKilometrajeServicio viajes = mock(BitacoraKilometrajeServicio.class);
    private final UUID viajeId = UUID.randomUUID();
    private final UUID busId = UUID.randomUUID();

    private <T> EventoRecibido<T> evento(String tipo, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, viajeId, 2L, Instant.now(), FECHA,
                UUID.randomUUID(), payload);
    }

    @Test
    void viajeIniciadoPoneElBusEnRuta() {
        ManejadorViajeIniciado manejador = new ManejadorViajeIniciado(viajes);
        ViajeIniciado viaje = new ViajeIniciado(viajeId, busId, OffsetDateTime.parse("2026-10-20T08:00:00-06:00"));

        manejador.manejar(evento("ViajeIniciado", viaje));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeIniciado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeIniciado.class);
        verify(viajes).registrarViajeIniciado(busId, viajeId, FECHA);
    }

    @Test
    void viajeCompletadoRegistraElKilometraje() {
        ManejadorViajeCompletado manejador = new ManejadorViajeCompletado(viajes);
        OffsetDateTime llegada = OffsetDateTime.parse("2026-10-20T12:00:00-06:00");
        ViajeCompletado viaje = new ViajeCompletado(viajeId, busId, new BigDecimal("1000.00"),
                new BigDecimal("1200.00"), llegada);

        manejador.manejar(evento("ViajeCompletado", viaje));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeCompletado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeCompletado.class);
        verify(viajes).registrarViajeCompletado(busId, viajeId, new BigDecimal("1000.00"),
                new BigDecimal("1200.00"), llegada, FECHA);
    }
}
