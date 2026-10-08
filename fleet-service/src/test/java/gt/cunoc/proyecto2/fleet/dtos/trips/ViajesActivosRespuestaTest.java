package gt.cunoc.proyecto2.fleet.dtos.trips;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViajesActivosRespuestaTest {

    @Test
    void sinViajes() {
        ViajesActivosRespuesta respuesta = new ViajesActivosRespuesta(0, null);

        assertThat(respuesta.hayViajes()).isFalse();
        assertThat(respuesta.viajes()).isEmpty();
    }

    @Test
    void conViajes() {
        var viaje = new ViajesActivosRespuesta.ViajeActivo(UUID.randomUUID(), "PROGRAMADO", OffsetDateTime.now());

        ViajesActivosRespuesta respuesta = new ViajesActivosRespuesta(1, List.of(viaje));

        assertThat(respuesta.hayViajes()).isTrue();
        assertThat(respuesta.viajes()).containsExactly(viaje);
    }
}
