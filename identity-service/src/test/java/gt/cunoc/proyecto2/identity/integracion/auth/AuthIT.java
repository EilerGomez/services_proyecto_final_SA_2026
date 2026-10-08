package gt.cunoc.proyecto2.identity.integracion.auth;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Flujo completo de sesión: registro, login, refresh, recuperación y doble factor. */
class AuthIT extends PruebaIntegracion {

    @Test
    void registroLoginYPerfil() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");

        ResponseEntity<String> login = post("/identity/auth/login", null,
                "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA));
        assertThat(login.getStatusCode().value() == 200).isTrue();
        String token = json(login, "$.tokenAcceso");
        assertThat(PruebaIntegracion.<String>json(login, "$.usuario.roles[0]")).isEqualTo("CLIENTE");

        ResponseEntity<String> yo = get("/identity/perfil", token);
        assertThat(yo.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<String>json(yo, "$.usuarioId")).isEqualTo(usuarioId.toString());
    }

    @Test
    void loginMalasCredencialesYBloqueoAlQuintoIntento() {
        registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String mal = "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"otra#2026\"}";

        for (int intento = 1; intento <= 4; intento++) {
            esError(post("/identity/auth/login", null, mal), 401, "CREDENCIALES_INVALIDAS");
        }
        esError(post("/identity/auth/login", null, mal), 401, "CREDENCIALES_INVALIDAS");
        esError(post("/identity/auth/login", null,
                "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA)),
                401, "CUENTA_BLOQUEADA");
    }

    @Test
    void refrescarRotaYRevocaElAnterior() {
        registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String refresco = json(post("/identity/auth/login", null,
                "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA)),
                "$.tokenRefresco");

        ResponseEntity<String> renovada = post("/identity/auth/refrescar", null,
                "{\"tokenRefresco\":\"%s\"}".formatted(refresco));
        assertThat(renovada.getStatusCode().value() == 200).isTrue();

        esError(post("/identity/auth/refrescar", null,
                "{\"tokenRefresco\":\"%s\"}".formatted(refresco)), 401, "TOKEN_REFRESCO_INVALIDO");
    }

    @Test
    void recuperarPublicaEventoConEnlace() {
        registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");

        ResponseEntity<String> respuesta = post("/identity/auth/recuperar", null,
                "{\"correo\":\"ana@pruebas.gt\"}");
        assertThat(respuesta.getStatusCode().value() == 202).isTrue();

        Map<String, String> eventos = esperarEventos("RecuperacionContrasenaSolicitada");
        assertThat(eventos.get("RecuperacionContrasenaSolicitada")).contains("/restablecer?token=");
    }

    @Test
    void dobleFactorExigeCodigoAntesDeLosTokens() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = json(post("/identity/auth/login", null,
                "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA)),
                "$.tokenAcceso");

        ResponseEntity<String> activado = put("/identity/auth/doble-factor", token, "{\"activado\":true}");
        assertThat(activado.getStatusCode().value() == 204).isTrue();

        ResponseEntity<String> desafio = post("/identity/auth/login", null,
                "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA));
        assertThat(desafio.getStatusCode().value() == 202).isTrue();
        esperarEventos("CodigoDobleFactorSolicitado");
        assertThat(contar("codigos_doble_factor")).isEqualTo(1);
        assertThat(usuarioId).isNotNull();
    }
}
