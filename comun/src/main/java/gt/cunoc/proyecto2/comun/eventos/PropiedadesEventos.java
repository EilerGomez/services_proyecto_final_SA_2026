package gt.cunoc.proyecto2.comun.eventos;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de eventos del servicio (application.yml):
 *
 * <pre>
 * comun:
 *   eventos:
 *     exchange: sales.eventos          # exchange propio (solo si publica)
 *     cola: sales.q                    # cola propia (solo si consume)
 *     suscripciones:                   # exchange de origen → eventos que escucha
 *       "[trips.eventos]": [TripScheduled, TripUpdated]
 *     outbox:
 *       intervalo-ms: 1000
 *       lote: 100
 * </pre>
 */
@ConfigurationProperties(prefix = "comun.eventos")
public record PropiedadesEventos(
        String exchange,
        String cola,
        Map<String, List<String>> suscripciones,
        @DefaultValue Outbox outbox) {

    public PropiedadesEventos {
        suscripciones = suscripciones == null ? Map.of() : suscripciones;
    }

    public record Outbox(
            @DefaultValue("1000") long intervaloMs,
            @DefaultValue("100") int lote) {
    }
}
