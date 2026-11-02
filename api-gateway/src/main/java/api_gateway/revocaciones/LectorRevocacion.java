package api_gateway.revocaciones;

import java.math.BigDecimal;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Clase encargada de procesar la información de revocación de tokens recibida desde identidad.
 *
 * Cuando la información contiene los campos requeridos y sus valores son válidos, registra la
 * fecha a partir de la cual los tokens se consideran válidos.
 *
 * Si los datos están ausentes, son nulos o presentan un formato
 * inválido, la revocación se ignora sin interrumpir el procesamiento.
 */
final class LectorRevocacion {

    // Impide instanciar esta clase utilitaria.
    private LectorRevocacion() {
    }

    // Registra la revocación si el nodo trae los dos datos; si no, la ignora y devuelve false.
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

    // Identifica campos JSON ausentes o nulos.
    private static boolean vacio(JsonNode valor) {
        return valor.isMissingNode() || valor.isNull();
    }

    // Convierte el instante JSON desde ISO-8601 o desde segundos Unix.
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
