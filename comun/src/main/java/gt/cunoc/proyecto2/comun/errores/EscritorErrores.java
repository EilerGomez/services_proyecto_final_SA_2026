package gt.cunoc.proyecto2.comun.errores;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/* Construye un ErrorRespuesta y la escribe en la respuesta (tambien desde filtros). */
@Component
public class EscritorErrores {

    private final JsonMapper json;

    public EscritorErrores(JsonMapper json) {
        this.json = json;
    }

    public ErrorRespuesta crear(HttpStatus status, String codigo, String mensaje, String ruta,
                                List<ErrorRespuesta.ErrorCampo> errores) {
        return new ErrorRespuesta(status.value(), codigo, mensaje, ruta, Instant.now(),
                ContextoCorrelacion.actual().map(Object::toString).orElse(null), errores);
    }

    public ResponseEntity<ErrorRespuesta> respuesta(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request) { // version corta como no tienes permisos
        return respuesta(status, codigo, mensaje, request, List.of());
    }

    public ResponseEntity<ErrorRespuesta> respuesta(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request,
                                                    List<ErrorRespuesta.ErrorCampo> errores) {//version larga como campos con errores o varios errores
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(crear(status, codigo, mensaje, request.getRequestURI(), errores));
    }

    /* Para filtros, que se ejecutan antes de Spring MVC y no pasan por el @RestControllerAdvice. */
    public void escribir(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String codigo, String mensaje) throws IOException {// forma manual, el error lo entrega el mismo
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(),
                crear(status, codigo, mensaje, request.getRequestURI(), List.of()));
    }
}
