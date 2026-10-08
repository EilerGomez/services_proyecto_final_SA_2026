package gt.cunoc.proyecto2.fleet.eventos.recibidos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PerfilActualizadoTest {

    @Test
    void nombreCompletoSinEspaciosSobrantes() {
        PerfilActualizado perfil = new PerfilActualizado(UUID.randomUUID(), "  Ana Maria ", " Lopez  ",
                "ana@fleet.gt", null);

        assertThat(perfil.nombreCompleto()).isEqualTo("Ana Maria Lopez");
    }
}
