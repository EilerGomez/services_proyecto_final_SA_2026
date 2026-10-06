package gt.cunoc.proyecto2.comun.errores;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte cualquier excepción en una {@link ErrorRespuesta} con el mismo formato en todos los servicios.
 * Incluye los errores lanzados por los triggers de PostgreSQL (RAISE EXCEPTION → SQLSTATE P0001).
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    private final EscritorErrores errores;

    public ManejadorGlobalErrores(EscritorErrores errores) {
        this.errores = errores;
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorRespuesta> negocio(NegocioException e, HttpServletRequest req) {
        return errores.respuesta(e.getStatus(), e.getCodigo(), e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoInvalido(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErrorRespuesta.ErrorCampo> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorRespuesta.ErrorCampo(f.getField(), f.getDefaultMessage()))
                .toList();
        return errores.respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Hay campos con errores", req, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorRespuesta> parametrosInvalidos(HandlerMethodValidationException e, HttpServletRequest req) {
        List<ErrorRespuesta.ErrorCampo> campos = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .map(m -> new ErrorRespuesta.ErrorCampo(null, m))
                .toList();
        return errores.respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Hay parámetros con errores", req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> restricciones(ConstraintViolationException e, HttpServletRequest req) {
        List<ErrorRespuesta.ErrorCampo> campos = e.getConstraintViolations().stream()
                .map(v -> new ErrorRespuesta.ErrorCampo(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return errores.respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Hay datos con errores", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos", req);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorRespuesta> parametro(Exception e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", e.getMessage(), req);
    }

    /** El archivo supera {@code spring.servlet.multipart.max-file-size} (Spring lo rechaza antes del controlador). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorRespuesta> archivoMuyGrande(MaxUploadSizeExceededException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "ARCHIVO_INVALIDO",
                "El archivo supera el tamaño máximo permitido", req);
    }

    /** Falta la parte del formulario (p. ej. el campo {@code archivo}) en una petición multipart. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorRespuesta> archivoFaltante(MissingServletRequestPartException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "ARCHIVO_INVALIDO",
                "Falta el archivo '" + e.getRequestPartName() + "'", req);
    }

    /** Petición multipart mal formada o que no es multipart. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorRespuesta> multipartInvalido(MultipartException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "ARCHIVO_INVALIDO",
                "La solicitud debe enviarse como multipart/form-data con el archivo", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.NOT_FOUND, "RUTA_INEXISTENTE", "La ruta no existe", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", e.getMessage(), req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorRespuesta> concurrencia(OptimisticLockingFailureException e, HttpServletRequest req) {
        return errores.respuesta(HttpStatus.CONFLICT, "MODIFICACION_CONCURRENTE",
                "Otro usuario modificó el registro; vuelva a cargarlo e intente de nuevo", req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorRespuesta> baseDeDatos(DataAccessException e, HttpServletRequest req) {
        SQLException sql = buscarSqlException(e);
        String estado = sql != null ? sql.getSQLState() : null;
        if ("P0001".equals(estado)) {                       // RAISE EXCEPTION de un trigger
            return errores.respuesta(HttpStatus.UNPROCESSABLE_CONTENT, "REGLA_NEGOCIO", mensajeTrigger(sql), req);
        }
        if ("23505".equals(estado)) {
            return errores.respuesta(HttpStatus.CONFLICT, "REGISTRO_DUPLICADO",
                    "Ya existe un registro con esos datos únicos", req);
        }
        if ("23503".equals(estado)) {
            return errores.respuesta(HttpStatus.CONFLICT, "REGISTRO_EN_USO",
                    "El registro está referenciado por otros datos", req);
        }
        if (estado != null && estado.startsWith("23")) {
            return errores.respuesta(HttpStatus.UNPROCESSABLE_CONTENT, "DATOS_INVALIDOS",
                    "Los datos no cumplen las restricciones de la base de datos", req);
        }
        log.error("Error de base de datos", e);
        return errores.respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_BASE_DATOS",
                "Error al acceder a la base de datos", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), e);
        return errores.respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado", req);
    }

    private static SQLException buscarSqlException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sql) {
                return sql;
            }
            t = t.getCause();
        }
        return null;
    }

    private static String mensajeTrigger(SQLException sql) {
        String mensaje = sql.getMessage() == null ? "" : sql.getMessage().lines().findFirst().orElse("");
        return mensaje.replaceFirst("^ERROR:\\s*", "");
    }
}