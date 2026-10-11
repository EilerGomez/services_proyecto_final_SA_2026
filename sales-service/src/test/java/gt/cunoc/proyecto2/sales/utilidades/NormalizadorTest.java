package gt.cunoc.proyecto2.sales.utilidades;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizadorTest {

    @Test
    void textoQuitaEspaciosDeLosExtremos() {
        assertThat(Normalizador.texto("  Ana  López ")).isEqualTo("Ana  López");
        assertThat(Normalizador.texto("   ")).isEmpty();
        assertThat(Normalizador.texto(null)).isNull();
    }

    @Test
    void textoOpcionalVacioEsNull() {
        assertThat(Normalizador.textoOpcional("  hola ")).isEqualTo("hola");
        assertThat(Normalizador.textoOpcional("   ")).isNull();
        assertThat(Normalizador.textoOpcional("")).isNull();
        assertThat(Normalizador.textoOpcional(null)).isNull();
    }

    @Test
    void codigoEnMayusculas() {
        assertThat(Normalizador.codigo(" 1234567-k ")).isEqualTo("1234567-K");
        assertThat(Normalizador.codigo(" cf ")).isEqualTo("CF");
        assertThat(Normalizador.codigo(null)).isNull();
    }

    @Test
    void correoEnMinusculas() {
        assertThat(Normalizador.correo("  Ana.Lopez@Correo.COM ")).isEqualTo("ana.lopez@correo.com");
        assertThat(Normalizador.correo(null)).isNull();
    }
}
