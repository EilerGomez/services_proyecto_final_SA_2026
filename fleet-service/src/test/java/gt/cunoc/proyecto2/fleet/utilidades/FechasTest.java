package gt.cunoc.proyecto2.fleet.utilidades;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FechasTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);

    @Test
    void inicioYFinDelDiaEnGuatemala() {
        LocalDate dia = LocalDate.of(2026, 10, 20);

        assertThat(Fechas.inicioDelDia(dia)).isEqualTo(OffsetDateTime.of(2026, 10, 20, 0, 0, 0, 0, GUATEMALA));
        assertThat(Fechas.finDelDia(dia)).isEqualTo(OffsetDateTime.of(2026, 10, 21, 0, 0, 0, 0, GUATEMALA));
    }

    @Test
    void sinFechaUsaLosLimitesAmplios() {
        assertThat(Fechas.inicioDelDia(null)).isEqualTo(OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, GUATEMALA));
        assertThat(Fechas.finDelDia(null)).isEqualTo(OffsetDateTime.of(2101, 1, 1, 0, 0, 0, 0, GUATEMALA));
    }

    @Test
    void diaDeNegocioDeUnInstante() {
        // 03:00 UTC del 21 = 21:00 del 20 en Guatemala
        OffsetDateTime instante = OffsetDateTime.of(2026, 10, 21, 3, 0, 0, 0, ZoneOffset.UTC);

        assertThat(Fechas.diaEnGuatemala(instante)).isEqualTo(LocalDate.of(2026, 10, 20));
    }
}
