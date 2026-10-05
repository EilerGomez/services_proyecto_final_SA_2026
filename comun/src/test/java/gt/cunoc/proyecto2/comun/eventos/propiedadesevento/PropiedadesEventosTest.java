package gt.cunoc.proyecto2.comun.eventos.propiedadesevento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropiedadesEventosTest {

    @Test
    void sinSuscripcionesQuedaUnMapaVacio() {
        PropiedadesEventos p = new PropiedadesEventos("sales.eventos", "sales.q", null,
                new PropiedadesEventos.Outbox(1000, 100));

        assertThat(p.suscripciones()).isEmpty();
        assertThat(p.outbox().intervaloMs()).isEqualTo(1000);
        assertThat(p.outbox().lote()).isEqualTo(100);
    }

    @Test
    void conservaLasSuscripciones() {
        PropiedadesEventos p = new PropiedadesEventos(null, "reporting.q",
                Map.of("trips.eventos", List.of("ViajeCompletado")), new PropiedadesEventos.Outbox(500, 10));

        assertThat(p.exchange()).isNull();
        assertThat(p.cola()).isEqualTo("reporting.q");
        assertThat(p.suscripciones()).containsEntry("trips.eventos", List.of("ViajeCompletado"));
    }
}
