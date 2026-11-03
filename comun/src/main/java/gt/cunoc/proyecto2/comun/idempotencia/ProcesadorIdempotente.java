package gt.cunoc.proyecto2.comun.idempotencia;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * Garantiza que cada evento se procese una sola vez aunque RabbitMQ lo entregue repetido.
 *Registra el eventId en {@code eventos_procesados} y ejecuta la acción en la MISMA transacción.
 * Si la acción falla, se revierte también el registro y el evento se podrá reintentar.
 * Si dos entregas llegan a la vez, la PK hace que solo una inserte.
 */
@Component
public class ProcesadorIdempotente {

    private static final String REGISTRAR = """
            INSERT INTO eventos_procesados (evento_id, tipo_evento)
            VALUES (?, ?)
            ON CONFLICT (evento_id) DO NOTHING
            """;

    private final JdbcTemplate jdbc;

    public ProcesadorIdempotente(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /*return true si se procesó; false si era un duplicado y se ignoró. */
    @Transactional
    public boolean procesar(UUID eventoId, String tipoEvento, Runnable accion) {
        int insertados = jdbc.update(REGISTRAR, eventoId, tipoEvento);
        if (insertados == 0) {
            return false;
        }
        accion.run();
        return true;
    }
}
