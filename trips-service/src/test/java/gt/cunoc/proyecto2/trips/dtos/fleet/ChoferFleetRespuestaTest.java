package gt.cunoc.proyecto2.trips.dtos.fleet;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChoferFleetRespuestaTest {

    private static ChoferFleetRespuesta chofer(String estado) {
        return new ChoferFleetRespuesta(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Pedro Gómez",
                estado, new BigDecimal("350.00"), LocalDate.of(2027, 1, 1), true);
    }

    @Test
    void soloElEstadoActivoEsActivo() {
        assertThat(chofer("ACTIVO").esActivo()).isTrue();
        assertThat(chofer("INACTIVO").esActivo()).isFalse();
        assertThat(chofer(null).esActivo()).isFalse();
    }
}
