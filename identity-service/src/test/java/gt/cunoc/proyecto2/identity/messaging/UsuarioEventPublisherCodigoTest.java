package gt.cunoc.proyecto2.identity.messaging;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.identity.evento.payload.CodigoDobleFactorPayload;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/** UsuarioEventPublisher.publicarCodigoDobleFactor: método sin cubrir. */
class UsuarioEventPublisherCodigoTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 20);
    private static final Instant EXPIRACION = Instant.parse("2026-09-21T12:00:00Z");

    private final PublicadorEventos outbox = mock(PublicadorEventos.class);
    private final UsuarioEventPublisher publisher = new UsuarioEventPublisher(outbox);

    @Test
    void publicarCodigoDobleFactorConservaContrato() {
        publisher.publicarCodigoDobleFactor(
                USUARIO, "correo", "nombre", "123456", EXPIRACION, FECHA);

        verify(outbox).publicar("Usuario", USUARIO, "CodigoDobleFactorSolicitado",
                new CodigoDobleFactorPayload(USUARIO, "correo", "nombre", "123456", EXPIRACION), FECHA);
        verifyNoMoreInteractions(outbox);
    }
}
