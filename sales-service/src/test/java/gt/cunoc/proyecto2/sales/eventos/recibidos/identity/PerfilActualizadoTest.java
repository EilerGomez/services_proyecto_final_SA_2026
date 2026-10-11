package gt.cunoc.proyecto2.sales.eventos.recibidos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PerfilActualizadoTest {

    private static PerfilActualizado perfil(String nombres, String apellidos) {
        return new PerfilActualizado(UUID.randomUUID(), "1234567-8", nombres, apellidos, "ana@correo.com");
    }

    @Test
    void nombreCompletoUneNombresYApellidos() {
        assertThat(perfil("Ana María", "López Pérez").nombreCompleto()).isEqualTo("Ana María López Pérez");
    }

    @Test
    void nombreCompletoQuitaEspaciosDeCadaParte() {
        assertThat(perfil("  Ana ", " López  ").nombreCompleto()).isEqualTo("Ana López");
    }

    @Test
    void nombreCompletoSinLasPartesNulas() {
        assertThat(perfil(null, "López").nombreCompleto()).isEqualTo("López");
        assertThat(perfil("Ana", null).nombreCompleto()).isEqualTo("Ana");
        assertThat(perfil(null, null).nombreCompleto()).isEmpty();
    }

    @Test
    void nombreCompletoSinLasPartesEnBlanco() {
        assertThat(perfil("   ", "López").nombreCompleto()).isEqualTo("López");
        assertThat(perfil("Ana", "").nombreCompleto()).isEqualTo("Ana");
        assertThat(perfil(" ", "  ").nombreCompleto()).isEmpty();
    }
}
