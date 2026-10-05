package gt.cunoc.proyecto2.comun.errores;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class EscritorErroresTest {

    private final EscritorErrores escritor = new EscritorErrores(JsonMapper.builder().build());

    @AfterEach
    void limpiar() {
        ContextoCorrelacion.limpiar();
    }

    @Test
    void creaLaRespuestaConElIdDeCorrelacionActual() {
        UUID correlacion = UUID.randomUUID();
        ContextoCorrelacion.establecer(correlacion);

        ErrorRespuesta r = escritor.crear(HttpStatus.CONFLICT, "ASIENTO_OCUPADO", "Ocupado", "/sales/tickets", List.of());

        assertThat(r.status()).isEqualTo(409);
        assertThat(r.codigo()).isEqualTo("ASIENTO_OCUPADO");
        assertThat(r.ruta()).isEqualTo("/sales/tickets");
        assertThat(r.correlationId()).isEqualTo(correlacion.toString());
        assertThat(r.timestamp()).isNotNull();
    }

    @Test
    void sinCorrelacionElCampoQuedaNulo() {
        ErrorRespuesta r = escritor.crear(HttpStatus.NOT_FOUND, "X", "m", "/r", null);
        assertThat(r.correlationId()).isNull();
        assertThat(r.errores()).isEmpty();
    }

    @Test
    void respuestaUsaLaRutaDeLaPeticion() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/fleet/buses");

        ResponseEntity<ErrorRespuesta> r = escritor.respuesta(HttpStatus.NOT_FOUND, "BUS_NO_EXISTE", "No existe", req);

        assertThat(r.getStatusCode().value()).isEqualTo(404);
        assertThat(r.getBody().ruta()).isEqualTo("/fleet/buses");
        assertThat(r.getBody().codigo()).isEqualTo("BUS_NO_EXISTE");
    }

    @Test
    void escribeElErrorComoJsonEnLaRespuesta() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/sales/tickets");
        MockHttpServletResponse res = new MockHttpServletResponse();

        escritor.escribir(req, res, HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "El token no es válido");

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).startsWith("application/json");
        assertThat(res.getContentAsString()).contains("\"codigo\":\"TOKEN_INVALIDO\"").contains("/sales/tickets");
    }
}
