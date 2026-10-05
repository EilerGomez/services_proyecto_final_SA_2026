package gt.cunoc.proyecto2.comun.errores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.json.JsonMapper;

class ManejadorGlobalErroresTest {

    private final ManejadorGlobalErrores manejador =
            new ManejadorGlobalErrores(new EscritorErrores(JsonMapper.builder().build()));
    private final MockHttpServletRequest req = new MockHttpServletRequest("POST", "/fleet/buses");

    @SuppressWarnings("unused")
    private void metodoDePrueba(String valor) {
    }

    @Test
    void excepcionDeNegocioUsaSuStatusYCodigo() {
        ResponseEntity<ErrorRespuesta> r =
                manejador.negocio(new ConflictoException("ASIENTO_OCUPADO", "El asiento 12 ya fue vendido"), req);

        assertThat(r.getStatusCode().value()).isEqualTo(409);
        assertThat(r.getBody().codigo()).isEqualTo("ASIENTO_OCUPADO");
        assertThat(r.getBody().mensaje()).isEqualTo("El asiento 12 ya fue vendido");
    }

    @Test
    void validacionDelCuerpoDevuelveLosCampos() throws Exception {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "solicitud");
        resultado.addError(new FieldError("solicitud", "placa", "no debe estar vacío"));
        MethodParameter parametro =
                new MethodParameter(getClass().getDeclaredMethod("metodoDePrueba", String.class), 0);

        ResponseEntity<ErrorRespuesta> r =
                manejador.cuerpoInvalido(new MethodArgumentNotValidException(parametro, resultado), req);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody().codigo()).isEqualTo("VALIDACION");
        assertThat(r.getBody().errores()).containsExactly(new ErrorRespuesta.ErrorCampo("placa", "no debe estar vacío"));
    }

    @Test
    void validacionDeParametrosDevuelveLosMensajes() {
        HandlerMethodValidationException e = mock(HandlerMethodValidationException.class);
        doReturn(List.of(new DefaultMessageSourceResolvable(new String[] {"Min"}, "debe ser mayor que 0")))
                .when(e).getAllErrors();

        ResponseEntity<ErrorRespuesta> r = manejador.parametrosInvalidos(e, req);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody().errores()).extracting(ErrorRespuesta.ErrorCampo::mensaje)
                .containsExactly("debe ser mayor que 0");
    }

    @Test
    void restriccionesDeBeanValidation() {
        ConstraintViolation<?> violacion = mock(ConstraintViolation.class);
        Path ruta = mock(Path.class);
        when(ruta.toString()).thenReturn("crear.placa");
        when(violacion.getPropertyPath()).thenReturn(ruta);
        when(violacion.getMessage()).thenReturn("no debe estar vacío");

        ResponseEntity<ErrorRespuesta> r =
                manejador.restricciones(new ConstraintViolationException(Set.of(violacion)), req);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody().errores()).containsExactly(new ErrorRespuesta.ErrorCampo("crear.placa", "no debe estar vacío"));
    }

    @Test
    void jsonInvalido() {
        ResponseEntity<ErrorRespuesta> r = manejador.jsonInvalido(mock(HttpMessageNotReadableException.class), req);
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody().codigo()).isEqualTo("CUERPO_INVALIDO");
    }

    @Test
    void parametroFaltante() {
        ResponseEntity<ErrorRespuesta> r =
                manejador.parametro(new MissingServletRequestParameterException("sucursalId", "UUID"), req);
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody().codigo()).isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void rutaInexistente() {
        ResponseEntity<ErrorRespuesta> r = manejador.rutaInexistente(mock(NoResourceFoundException.class), req);
        assertThat(r.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void metodoNoPermitido() {
        ResponseEntity<ErrorRespuesta> r = manejador.metodo(new HttpRequestMethodNotSupportedException("PATCH"), req);
        assertThat(r.getStatusCode().value()).isEqualTo(405);
    }

    @Test
    void bloqueoOptimista() {
        ResponseEntity<ErrorRespuesta> r =
                manejador.concurrencia(new OptimisticLockingFailureException("versión distinta"), req);
        assertThat(r.getStatusCode().value()).isEqualTo(409);
        assertThat(r.getBody().codigo()).isEqualTo("MODIFICACION_CONCURRENTE");
    }

    private ResponseEntity<ErrorRespuesta> errorBd(String mensaje, String estadoSql) {
        return manejador.baseDeDatos(
                new DataIntegrityViolationException("error", new SQLException(mensaje, estadoSql)), req);
    }

    @Test
    void triggerDePostgresDevuelve422ConSuMensaje() {
        ResponseEntity<ErrorRespuesta> r = errorBd(
                "ERROR: La sucursal debe tener al menos un administrador activo\n  Where: PL/pgSQL function", "P0001");

        assertThat(r.getStatusCode().value()).isEqualTo(422);
        assertThat(r.getBody().codigo()).isEqualTo("REGLA_NEGOCIO");
        assertThat(r.getBody().mensaje()).isEqualTo("La sucursal debe tener al menos un administrador activo");
    }

    @Test
    void erroresDeIntegridadDeLaBaseDeDatos() {
        assertThat(errorBd("duplicado", "23505").getBody().codigo()).isEqualTo("REGISTRO_DUPLICADO");
        assertThat(errorBd("fk", "23503").getBody().codigo()).isEqualTo("REGISTRO_EN_USO");
        assertThat(errorBd("check", "23514").getBody().codigo()).isEqualTo("DATOS_INVALIDOS");
        assertThat(errorBd("conexión", "08006").getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void errorDeBaseDeDatosSinSqlException() {
        ResponseEntity<ErrorRespuesta> r = manejador.baseDeDatos(new DataIntegrityViolationException("sin causa"), req);
        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody().codigo()).isEqualTo("ERROR_BASE_DATOS");
    }

    @Test
    void triggerSinMensaje() {
        ResponseEntity<ErrorRespuesta> r = errorBd(null, "P0001");
        assertThat(r.getStatusCode().value()).isEqualTo(422);
        assertThat(r.getBody().mensaje()).isEmpty();
    }

    @Test
    void errorInesperado() {
        ResponseEntity<ErrorRespuesta> r = manejador.inesperado(new IllegalStateException("falló"), req);
        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody().codigo()).isEqualTo("ERROR_INTERNO");
    }
}
