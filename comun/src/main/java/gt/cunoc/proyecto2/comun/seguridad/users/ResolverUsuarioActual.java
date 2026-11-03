package gt.cunoc.proyecto2.comun.seguridad.users;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.seguridad.filtro.FiltroAutenticacion;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/*
 * Permite declarar {UsuarioActual} como parámetro de un controlador.
 * Si no hay usuario autenticado responde 401, salvo que el parámetro sea opcional (@Nullable).
 */
public class ResolverUsuarioActual implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UsuarioActual.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object usuario = webRequest.getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO,
                RequestAttributes.SCOPE_REQUEST);
        if (usuario == null && !parameter.isOptional()) {
            throw new NoAutenticadoException("Debe iniciar sesión");
        }
        return usuario;
    }
}
