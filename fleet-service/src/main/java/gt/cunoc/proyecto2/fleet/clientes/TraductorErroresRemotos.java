package gt.cunoc.proyecto2.fleet.clientes;

import gt.cunoc.proyecto2.comun.errores.ErrorRespuesta;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Convierte un 4xx de otro servicio en una excepción de comun.
 * 400, 409 y 422 son errores del usuario (correo en uso, DPI repetido...): pasan tal cual,
 * con el mismo código y mensaje. Cualquier otro 4xx (401, 403, 404) es un problema de
 * configuración entre servicios: se registra y el usuario recibe 503.
 */
public final class TraductorErroresRemotos {

    private static final Logger log = LoggerFactory.getLogger(TraductorErroresRemotos.class);
    private static final Set<Integer> ERRORES_DEL_USUARIO = Set.of(400, 409, 422);

    private TraductorErroresRemotos() {
    }

    public static NegocioException traducir(HttpClientErrorException e, CodigoError siNoSeReconoce) {
        int status = e.getStatusCode().value();
        ErrorRespuesta cuerpo = leerCuerpo(e);
        if (!ERRORES_DEL_USUARIO.contains(status) || cuerpo == null || cuerpo.codigo() == null) {
            log.error("Respuesta inesperada de otro servicio: {} {}", status, e.getResponseBodyAsString());
            return siNoSeReconoce.excepcion();
        }
        return new NegocioException(HttpStatus.valueOf(status), cuerpo.codigo(), cuerpo.mensaje());
    }

    private static ErrorRespuesta leerCuerpo(HttpClientErrorException e) {
        try {
            return e.getResponseBodyAs(ErrorRespuesta.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}