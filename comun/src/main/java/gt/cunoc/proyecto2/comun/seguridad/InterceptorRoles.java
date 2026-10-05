package gt.cunoc.proyecto2.comun.seguridad;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Aplica {@link RequiereRol}. Las excepciones las convierte el manejador global en 401/403. */
public class InterceptorRoles implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        RequiereRol requisito = AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(), RequiereRol.class);
        if (requisito == null) {
            requisito = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), RequiereRol.class);
        }
        if (requisito == null) {
            return true;
        }

        UsuarioActual usuario = (UsuarioActual) request.getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO);
        if (usuario == null) {
            throw new NoAutenticadoException("Debe iniciar sesión");
        }
        if (requisito.value().length > 0 && !usuario.tieneAlgunRol(requisito.value())) {
            throw new ProhibidoException("ROL_INSUFICIENTE", "Su rol no permite realizar esta operación");
        }
        return true;
    }
}
