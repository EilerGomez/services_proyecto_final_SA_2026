package gt.cunoc.proyecto2.comun.eventos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.eventos.propiedadesevento.PropiedadesEventos;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class ConfiguracionEventosTest {

    private final ConfiguracionEventos configuracion = new ConfiguracionEventos();

    @Test
    void declaraExchangePropioColaDlqYBindings() {
        Map<String, List<String>> suscripciones = new LinkedHashMap<>();
        suscripciones.put("trips.eventos", List.of("ViajeProgramado", "ViajeEliminado"));
        suscripciones.put("identity.eventos", List.of("CarteraDebitada"));
        PropiedadesEventos propiedades = new PropiedadesEventos("sales.eventos", "sales.q", suscripciones,
                new PropiedadesEventos.Outbox(1000, 100));

        Declarables declarables = configuracion.topologiaEventos(propiedades);

        assertThat(declarables.getDeclarablesByType(TopicExchange.class))
                .extracting(TopicExchange::getName)
                .containsExactlyInAnyOrder("sales.eventos", "trips.eventos", "identity.eventos");

        List<Queue> colas = declarables.getDeclarablesByType(Queue.class);
        assertThat(colas).extracting(Queue::getName).containsExactlyInAnyOrder("sales.q", "sales.q.dlq");
        Queue cola = colas.stream().filter(q -> q.getName().equals("sales.q")).findFirst().orElseThrow();
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", "")
                .containsEntry("x-dead-letter-routing-key", "sales.q.dlq");

        assertThat(declarables.getDeclarablesByType(Binding.class))
                .extracting(Binding::getRoutingKey)
                .containsExactlyInAnyOrder("ViajeProgramado", "ViajeEliminado", "CarteraDebitada");
    }

    @Test
    void servicioQueSoloConsumeNoDeclaraExchangePropio() {
        PropiedadesEventos propiedades = new PropiedadesEventos(null, "reporting.q",
                Map.of("sales.eventos", List.of("BoletoVendido")), new PropiedadesEventos.Outbox(1000, 100));

        Declarables declarables = configuracion.topologiaEventos(propiedades);

        assertThat(declarables.getDeclarablesByType(TopicExchange.class))
                .extracting(TopicExchange::getName).containsExactly("sales.eventos");
    }

    @Test
    void servicioQueSoloPublicaNoDeclaraCola() {
        PropiedadesEventos propiedades = new PropiedadesEventos("maintenance.eventos", null, Map.of(),
                new PropiedadesEventos.Outbox(1000, 100));

        Declarables declarables = configuracion.topologiaEventos(propiedades);

        assertThat(declarables.getDeclarablesByType(Queue.class)).isEmpty();
        assertThat(declarables.getDeclarablesByType(TopicExchange.class)).hasSize(1);
    }

    @Test
    void sinConfiguracionNoDeclaraNada() {
        PropiedadesEventos propiedades = new PropiedadesEventos(null, null, null,
                new PropiedadesEventos.Outbox(1000, 100));

        assertThat(configuracion.topologiaEventos(propiedades).getDeclarables()).isEmpty();
    }
}
