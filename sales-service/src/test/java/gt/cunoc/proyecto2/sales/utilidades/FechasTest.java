package gt.cunoc.proyecto2.sales.utilidades;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FechasTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);

    @Test
    void hoyEsLaFechaDeGuatemala() {
        assertThat(Fechas.hoy()).isEqualTo(LocalDate.now(Fechas.ZONA_GUATEMALA));
    }

    @Test
    void ahoraEsEsteMomentoConLaHoraDeGuatemala() {
        Instant antes = Instant.now();

        OffsetDateTime ahora = Fechas.ahora();

        Instant despues = Instant.now();
        assertThat(ahora.getOffset()).isEqualTo(GUATEMALA);
        assertThat(ahora.toInstant()).isBetween(antes, despues);
    }

    @Test
    void enGuatemalaCambiaElOffsetSinCambiarElInstante() {
        OffsetDateTime utc = OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.UTC);

        OffsetDateTime local = Fechas.enGuatemala(utc);

        assertThat(local).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
        assertThat(local.getOffset()).isEqualTo(GUATEMALA);
        assertThat(local).isAtSameInstantAs(utc);
    }

    @Test
    void enGuatemalaDeUnaHoraYaLocalNoCambia() {
        OffsetDateTime local = OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA);

        assertThat(Fechas.enGuatemala(local)).isEqualTo(local);
    }

    @Test
    void enGuatemalaDeNullEsNull() {
        assertThat(Fechas.enGuatemala(null)).isNull();
    }
}
