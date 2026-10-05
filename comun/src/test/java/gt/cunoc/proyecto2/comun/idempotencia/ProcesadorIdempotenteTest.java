package gt.cunoc.proyecto2.comun.idempotencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ProcesadorIdempotenteTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ProcesadorIdempotente procesador = new ProcesadorIdempotente(jdbc);
    private final AtomicInteger ejecuciones = new AtomicInteger();

    @Test
    void laPrimeraVezEjecutaLaAccion() {
        UUID id = UUID.randomUUID();
        when(jdbc.update(anyString(), eq(id), eq("BoletoVendido"))).thenReturn(1);

        boolean procesado = procesador.procesar(id, "BoletoVendido", ejecuciones::incrementAndGet);

        assertThat(procesado).isTrue();
        assertThat(ejecuciones).hasValue(1);
    }

    @Test
    void unEventoRepetidoNoSeEjecuta() {
        UUID id = UUID.randomUUID();
        when(jdbc.update(anyString(), eq(id), eq("BoletoVendido"))).thenReturn(0);

        boolean procesado = procesador.procesar(id, "BoletoVendido", ejecuciones::incrementAndGet);

        assertThat(procesado).isFalse();
        assertThat(ejecuciones).hasValue(0);
    }
}
