package gt.cunoc.proyecto2.comun.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Verifica que el sobre viaja como JSON y que el payload se convierte al record del consumidor. */
class EventoSobreJsonTest {

    record BoletoVendido(UUID ventaId, List<Integer> asientos, BigDecimal montoTotal, String metodoPago) {
    }

    /** El consumidor declara solo los campos que usa: los demás se ignoran. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record BoletoVendidoResumen(UUID ventaId, BigDecimal montoTotal) {
    }

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void elSobreSeSerializaYSeLeeSinPerderDatos() {
        UUID ventaId = UUID.randomUUID();
        EventoSobre original = new EventoSobre(UUID.randomUUID(), "TicketSold", 1, ventaId, 3L,
                Instant.parse("2026-10-20T15:30:12Z"), LocalDate.parse("2026-10-20"), UUID.randomUUID(),
                new BoletoVendido(ventaId, List.of(12, 13), new BigDecimal("150.00"), "CARTERA"));

        String texto = json.writeValueAsString(original);
        EventoSobre leido = json.readValue(texto, EventoSobre.class);

        assertThat(texto).contains("\"businessDate\":\"2026-10-20\"");
        assertThat(leido.eventId()).isEqualTo(original.eventId());
        assertThat(leido.eventType()).isEqualTo("TicketSold");
        assertThat(leido.aggregateVersion()).isEqualTo(3L);
        assertThat(leido.occurredAt()).isEqualTo(original.occurredAt());
        assertThat(leido.businessDate()).isEqualTo(original.businessDate());

        BoletoVendidoResumen payload = json.convertValue(leido.payload(), BoletoVendidoResumen.class);
        assertThat(payload.ventaId()).isEqualTo(ventaId);
        assertThat(payload.montoTotal()).isEqualByComparingTo("150.00");
    }
}
