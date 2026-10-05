package gt.cunoc.proyecto2.comun.eventos.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.eventos.propiedadesevento.PropiedadesEventos;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class RelayOutboxTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final PlatformTransactionManager transacciones = mock(PlatformTransactionManager.class);
    private RelayOutbox relay;

    @BeforeEach
    void preparar() {
        when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        PropiedadesEventos propiedades = new PropiedadesEventos("sales.eventos", null, Map.of(),
                new PropiedadesEventos.Outbox(1000, 100));
        relay = new RelayOutbox(jdbc, rabbit, transacciones, propiedades);
    }

    /** Simula las filas pendientes de eventos_salida (ejecuta el RowMapper real del relay). */
    private void pendientes(UUID... ids) {
        doAnswer(invocacion -> {
            RowMapper<?> mapper = invocacion.getArgument(1);
            List<Object> filas = new ArrayList<>();
            for (UUID id : ids) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getObject("id", UUID.class)).thenReturn(id);
                when(rs.getString("tipo_evento")).thenReturn("BoletoVendido");
                when(rs.getString("datos")).thenReturn("{\"eventId\":\"" + id + "\"}");
                filas.add(mapper.mapRow(rs, filas.size()));
            }
            return filas;
        }).when(jdbc).query(anyString(), any(RowMapper.class), eq(100));
    }

    /** RabbitMQ responde a cada publicación con ack o nack. */
    private void rabbitConfirma(boolean ack) {
        doAnswer(invocacion -> {
            CorrelationData confirmacion = invocacion.getArgument(3);
            confirmacion.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack del broker"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void publicaLosPendientesYLosMarcaComoPublicados() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        pendientes(id1, id2);
        rabbitConfirma(true);

        relay.publicarPendientes();

        ArgumentCaptor<Message> mensajes = ArgumentCaptor.forClass(Message.class);
        verify(rabbit, times(2)).send(eq("sales.eventos"), eq("BoletoVendido"), mensajes.capture(),
                any(CorrelationData.class));
        Message primero = mensajes.getAllValues().get(0);
        assertThat(primero.getMessageProperties().getMessageId()).isEqualTo(id1.toString());
        assertThat(primero.getMessageProperties().getType()).isEqualTo("BoletoVendido");
        assertThat(primero.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(primero.getBody(), StandardCharsets.UTF_8)).contains(id1.toString());

        verify(jdbc).update(contains("fecha_publicacion = now()"), eq(id1));
        verify(jdbc).update(contains("fecha_publicacion = now()"), eq(id2));
    }

    @Test
    void siRabbitRechazaRegistraElFalloYDetieneElLote() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        pendientes(id1, id2);
        rabbitConfirma(false);

        relay.publicarPendientes();

        verify(rabbit, times(1)).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        verify(jdbc).update(contains("ultimo_error = ?"), contains("nack del broker"), eq(id1));
        verify(jdbc, never()).update(contains("fecha_publicacion = now()"), eq(id1));
    }

    @Test
    void siFallaElEnvioGuardaElErrorRecortadoA1000Caracteres() {
        UUID id = UUID.randomUUID();
        pendientes(id);
        doThrow(new IllegalStateException("x".repeat(2000)))
                .when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.publicarPendientes();

        ArgumentCaptor<Object> error = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(contains("ultimo_error = ?"), error.capture(), eq(id));
        assertThat((String) error.getValue()).hasSize(1000);
    }

    @Test
    void sinPendientesNoPublicaNada() {
        pendientes();

        relay.publicarPendientes();

        verify(rabbit, never()).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }
}
