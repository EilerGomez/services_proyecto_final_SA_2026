package gt.cunoc.proyecto2.identity.services.oauth;

import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Códigos opacos de un solo uso en memoria: states anti-CSRF del flujo OAuth
 * y tickets de canje (callback -&gt; frontend -&gt; sesión). No se persisten:
 * ante un reinicio solo se pierde el login en curso.
 */
@Service
public class CodigosUnSoloUso {

    /** Ticket canjeado: usuario listo para emitir sesión (o desafiar doble factor). */
    public record Ticket(UUID usuarioId, boolean dobleFactorPendiente) { }

    private record Entrada<T>(T valor, Instant expiraEn) { }

    private final Map<String, Entrada<Boolean>> estados = new ConcurrentHashMap<>();
    private final Map<String, Entrada<Ticket>> tickets = new ConcurrentHashMap<>();
    private final TokenOpacoService opacos;

    public CodigosUnSoloUso(TokenOpacoService opacos) {
        this.opacos = opacos;
    }

    public String emitirEstado(long ttlSegundos) {
        return emitir(estados, Boolean.TRUE, ttlSegundos);
    }

    /** Consume el state: null si no existe, ya se usó o venció. */
    public Boolean consumirEstado(String estado) {
        return consumir(estados, estado, Instant.now());
    }

    Boolean consumirEstado(String estado, Instant ahora) {
        return consumir(estados, estado, ahora);
    }

    public String emitirTicket(Ticket ticket, long ttlSegundos) {
        return emitir(tickets, ticket, ttlSegundos);
    }

    /** Consume el ticket: null si no existe, ya se usó o venció. */
    public Ticket consumirTicket(String ticket) {
        return consumir(tickets, ticket, Instant.now());
    }

    Ticket consumirTicket(String ticket, Instant ahora) {
        return consumir(tickets, ticket, ahora);
    }

    private <T> String emitir(Map<String, Entrada<T>> mapa, T valor, long ttlSegundos) {
        if (mapa.size() > 10_000) {
            Instant ahora = Instant.now();
            mapa.entrySet().removeIf(e -> !e.getValue().expiraEn().isAfter(ahora));
        }
        String codigo = opacos.generar();
        mapa.put(codigo, new Entrada<>(valor, Instant.now().plusSeconds(ttlSegundos)));
        return codigo;
    }

    private <T> T consumir(Map<String, Entrada<T>> mapa, String codigo, Instant ahora) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        Entrada<T> entrada = mapa.remove(codigo);
        if (entrada == null || !entrada.expiraEn().isAfter(ahora)) {
            return null;
        }
        return entrada.valor();
    }
}
