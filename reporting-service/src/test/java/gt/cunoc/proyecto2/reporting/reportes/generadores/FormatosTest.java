package gt.cunoc.proyecto2.reporting.reportes.generadores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FormatosTest {

    @Test
    void textoUsaTodosCuandoEsNulo() {
        assertThat(Formatos.texto(null)).isEqualTo("todos");
        assertThat(Formatos.texto("2026-10-01")).isEqualTo("2026-10-01");
    }

    @Test
    void fechaUsaGuionCuandoEsNula() {
        assertThat(Formatos.fecha(null)).isEqualTo("-");
        assertThat(Formatos.fecha(java.time.LocalDate.of(2026, 10, 1))).isEqualTo("2026-10-01");
    }

    @Test
    void rutaToleraOrigenYDestinoNulos() {
        assertThat(Formatos.ruta(null, null)).isEqualTo("- → -");
        assertThat(Formatos.ruta("Xela", null)).isEqualTo("Xela → -");
        assertThat(Formatos.ruta(null, "Guate")).isEqualTo("- → Guate");
        assertThat(Formatos.ruta("Xela", "Guate")).isEqualTo("Xela → Guate");
    }

    @Test
    void fechaHoraConvierteElInstanteAZonaGuatemala() {
        assertThat(Formatos.fechaHora(null)).isNull();
        // 08:00 en Guatemala llega como 14:00Z (Jackson normaliza a UTC): se vuelve a mostrar 08:00.
        assertThat(Formatos.fechaHora(java.time.OffsetDateTime.parse("2026-11-05T14:00:00Z")))
                .isEqualTo("05/11/2026 08:00");
    }

    @Test
    void quetzalesToleraNulo() {
        assertThat(Formatos.quetzales(null)).isEqualTo("Q 0.00");
        assertThat(Formatos.quetzales(new BigDecimal("10.50"))).isEqualTo("Q 10.50");
    }
}
