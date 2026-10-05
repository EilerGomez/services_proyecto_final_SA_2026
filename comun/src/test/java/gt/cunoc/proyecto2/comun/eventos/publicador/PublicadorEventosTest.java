package gt.cunoc.proyecto2.comun.eventos.publicador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class PublicadorEventosTest {

    record Pago(UUID ventaId, BigDecimal monto) {
    }

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PublicadorEventos publicador = new PublicadorEventos(jdbc, JsonMapper.builder().build());

    @AfterEach
    void limpiar() {
        ContextoCorrelacion.limpiar();
    }

    @Test
    void guardaElSobreCompletoEnElOutbox() {
        UUID ventaId = UUID.randomUUID();
        UUID correlacion = UUID.randomUUID();
        ContextoCorrelacion.establecer(correlacion);

        EventoSobre sobre = publicador.publicar("Venta", ventaId, 3L, "BoletoVendido",
                new Pago(ventaId, new BigDecimal("150.00")), LocalDate.parse("2026-10-20"));

        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), eq(sobre.eventId()), eq("Venta"), eq(ventaId), eq("BoletoVendido"),
                eq(1), datos.capture());

        assertThat(sobre.aggregateVersion()).isEqualTo(3L);
        assertThat(sobre.correlationId()).isEqualTo(correlacion);
        assertThat((String) datos.getValue())
                .contains("\"eventType\":\"BoletoVendido\"")
                .contains("\"aggregateVersion\":3")
                .contains("\"businessDate\":\"2026-10-20\"");
    }

    @Test
    void sinVersionNiFechaUsaNuloYLaFechaDeHoy() {
        UUID rutaId = UUID.randomUUID();

        EventoSobre sobre = publicador.publicar("Ruta", rutaId, "RutaEliminada", new Pago(rutaId, BigDecimal.ONE), null);

        assertThat(sobre.aggregateVersion()).isNull();
        assertThat(sobre.businessDate()).isEqualTo(LocalDate.now());
        assertThat(sobre.correlationId()).isNotNull();
        assertThat(sobre.version()).isEqualTo(1);
    }

    @Test
    void exigeLosDatosObligatorios() {
        UUID id = UUID.randomUUID();
        Object payload = new Pago(id, BigDecimal.ONE);
        LocalDate hoy = LocalDate.now();

        assertThatThrownBy(() -> publicador.publicar(null, id, "X", payload, hoy)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> publicador.publicar("Venta", null, "X", payload, hoy)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> publicador.publicar("Venta", id, null, payload, hoy)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> publicador.publicar("Venta", id, "X", null, hoy)).isInstanceOf(NullPointerException.class);
    }
}
