package api_gateway.revocaciones;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Revocaciones de sesión en memoria: por usuario, desde cuándo vuelven a valer sus JWT
 * ({@code tokensValidosDesde} de identity). Un token emitido ({@code iat}) antes de esa fecha se rechaza.
 * <p>
 * Se llena con el evento SesionRevocada y, al arrancar, con las revocaciones recientes de identity.
 * Una revocación más vieja que lo que dura un JWT ya no afecta a ningún token vigente y se descarta.
 */
@Component
public class RegistroRevocaciones {

    private final Map<String, Instant> validosDesde = new ConcurrentHashMap<>();
    private final Duration retencion;
    private final Clock reloj;

    // Inicializa el registro usando retención configurada y reloj del sistema.
    @Autowired
    public RegistroRevocaciones(@Value("${gateway.revocaciones.retencion:65m}") Duration retencion) {
        this(retencion, Clock.systemUTC());
    }

    // Permite probar el registro con reloj y retención controlados.
    RegistroRevocaciones(Duration retencion, Clock reloj) {
        this.retencion = retencion;
        this.reloj = reloj;
    }

    // Guarda la revocación; si ya había una del mismo usuario se queda la más reciente.
    // Registra la fecha de validez más reciente y purga entradas fuera de retención.
    public void registrar(String usuarioId, Instant tokensValidosDesde) {
        if (usuarioId == null || usuarioId.isBlank() || tokensValidosDesde == null) {
            return;
        }
        validosDesde.merge(usuarioId, tokensValidosDesde, (actual, nueva) -> nueva.isAfter(actual) ? nueva : actual);
        descartarVencidas();
    }

    // true si el token de ese usuario se emitió antes de su revocación (o no dice cuándo se emitió).
    // Comprueba si el instante de emisión precede a la fecha válida del usuario.
    public boolean revocado(String usuarioId, Instant emitido) {
        Instant desde = usuarioId == null ? null : validosDesde.get(usuarioId);
        return desde != null && (emitido == null || emitido.isBefore(desde));
    }

    // Devuelve el número de usuarios con una revocación vigente en memoria.
    public int cantidad() {
        return validosDesde.size();
    }

    // Elimina revocaciones antiguas que ya no pueden afectar JWT vigentes.
    void descartarVencidas() {
        Instant limite = reloj.instant().minus(retencion);
        validosDesde.values().removeIf(desde -> desde.isBefore(limite));
    }
}
