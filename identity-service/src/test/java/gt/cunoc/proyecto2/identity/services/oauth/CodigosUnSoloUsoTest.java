package gt.cunoc.proyecto2.identity.services.oauth;

import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** States y tickets de un solo uso: consumo único, vencimiento y limpieza. */
class CodigosUnSoloUsoTest {

    private final CodigosUnSoloUso codigos = new CodigosUnSoloUso(new TokenOpacoService());

    @Test
    void estadoSeConsumeUnaSolaVez() {
        String estado = codigos.emitirEstado(600);
        assertNotNull(codigos.consumirEstado(estado));
        assertNull(codigos.consumirEstado(estado));
        assertNull(codigos.consumirEstado("inexistente"));
        assertNull(codigos.consumirEstado(null));
        assertNull(codigos.consumirEstado("  "));
    }

    @Test
    void estadoVencidoSeRechaza() {
        String estado = codigos.emitirEstado(-1);
        assertNull(codigos.consumirEstado(estado, Instant.now()));
        String vigente = codigos.emitirEstado(600);
        assertNotNull(codigos.consumirEstado(vigente, Instant.now()));
    }

    @Test
    void ticketGuardaUsuarioYDobleFactor() {
        UUID id = UUID.randomUUID();
        String ticket = codigos.emitirTicket(new CodigosUnSoloUso.Ticket(id, true), 120);
        CodigosUnSoloUso.Ticket leido = codigos.consumirTicket(ticket, Instant.now());
        assertNotNull(leido);
        assertEquals(id, leido.usuarioId());
        assertEquals(true, leido.dobleFactorPendiente());
        assertNull(codigos.consumirTicket(ticket, Instant.now()));
        assertNull(codigos.consumirTicket("inexistente", Instant.now()));
    }

    @Test
    void purgaExpiradosCuandoCreceMucho() {
        for (int i = 0; i < 10_001; i++) {
            codigos.emitirEstado(-1);
        }
        String vigente = codigos.emitirEstado(600);
        assertNotNull(codigos.consumirEstado(vigente));
    }
}
