package api_gateway.revocaciones;

import java.math.BigDecimal;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Lee una revocación ({@code usuarioId}, {@code tokensValidosDesde}) del JSON de identity. */
final class LectorRevocacion {

    private LectorRevocacion() {
    }

    /** Registra la revocación si el nodo trae los dos datos; si no, la ignora y devuelve false. */
    static boolean registrar(JsonNode nodo, RegistroRevocaciones registro) {
        if (nodo == null) {
            return false;
        }
        JsonNode usuario = nodo.path("usuarioId");
        JsonNode valor = nodo.path("tokensValidosDesde");
        if (vacio(usuario) || vacio(valor)) {
            return false;
        }
        Instant desde = instante(valor);
        if (desde == null) {
            return false;
        }
        registro.registrar(usuario.asText(), desde);
        return true;
    }

    private static boolean vacio(JsonNode valor) {
        return valor.isMissingNode() || valor.isNull();
    }

    /** Jackson escribe un Instant como texto ISO-8601 o, según la configuración, como segundos (con fracción). */
    static Instant instante(JsonNode valor) {
        try {
            if (valor.isNumber()) {
                BigDecimal segundos = valor.decimalValue();
                return Instant.ofEpochSecond(segundos.longValue(),
                        segundos.remainder(BigDecimal.ONE).movePointRight(9).longValue());
            }
            return Instant.parse(valor.asText());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
