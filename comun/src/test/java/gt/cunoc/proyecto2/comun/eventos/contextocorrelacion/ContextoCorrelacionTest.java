package gt.cunoc.proyecto2.comun.eventos.contextocorrelacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ContextoCorrelacionTest {

    @AfterEach
    void limpiar() {
        ContextoCorrelacion.limpiar();
    }

    @Test
    void sinValorNoHayCorrelacionYSeGeneraUnaNueva() {
        assertThat(ContextoCorrelacion.actual()).isEmpty();
        assertThat(ContextoCorrelacion.actualOGenerar()).isNotNull();
    }

    @Test
    void establecerGuardaElIdYLoPoneEnLosLogs() {
        UUID id = UUID.randomUUID();
        ContextoCorrelacion.establecer(id);

        assertThat(ContextoCorrelacion.actual()).contains(id);
        assertThat(ContextoCorrelacion.actualOGenerar()).isEqualTo(id);
        assertThat(MDC.get(ContextoCorrelacion.CLAVE_MDC)).isEqualTo(id.toString());
    }

    @Test
    void limpiarQuitaElIdDelContextoYDeLosLogs() {
        ContextoCorrelacion.establecer(UUID.randomUUID());
        ContextoCorrelacion.limpiar();

        assertThat(ContextoCorrelacion.actual()).isEmpty();
        assertThat(MDC.get(ContextoCorrelacion.CLAVE_MDC)).isNull();
    }
}
