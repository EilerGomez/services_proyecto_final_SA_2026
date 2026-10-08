package gt.cunoc.proyecto2.fleet.utilidades;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NormalizadorTest {

    @Test
    void textoQuitaEspaciosDeLosExtremos() {
        assertThat(Normalizador.texto("  Xela Central  ")).isEqualTo("Xela Central");
        assertThat(Normalizador.texto(null)).isNull();
    }

    @Test
    void codigoQuedaEnMayusculas() {
        assertThat(Normalizador.codigo(" p-123abc ")).isEqualTo("P-123ABC");
        assertThat(Normalizador.codigo(null)).isNull();
    }
}
