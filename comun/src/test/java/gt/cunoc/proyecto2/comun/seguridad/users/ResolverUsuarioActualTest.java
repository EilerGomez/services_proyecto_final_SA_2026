package gt.cunoc.proyecto2.comun.seguridad.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.seguridad.filtro.FiltroAutenticacion;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ResolverUsuarioActualTest {

    /** Cualquier anotación llamada "Nullable" marca el parámetro como opcional para Spring. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Nullable {
    }

    @SuppressWarnings("unused")
    static class Ejemplo {
        void requerido(UsuarioActual usuario) {
        }

        void opcional(@Nullable UsuarioActual usuario) {
        }

        void otroTipo(String texto) {
        }
    }

    private final ResolverUsuarioActual resolver = new ResolverUsuarioActual();
    private final MockHttpServletRequest req = new MockHttpServletRequest();

    private static MethodParameter parametro(String metodo, Class<?> tipo) throws Exception {
        return new MethodParameter(Ejemplo.class.getDeclaredMethod(metodo, tipo), 0);
    }

    @Test
    void soloResuelveParametrosUsuarioActual() throws Exception {
        assertThat(resolver.supportsParameter(parametro("requerido", UsuarioActual.class))).isTrue();
        assertThat(resolver.supportsParameter(parametro("otroTipo", String.class))).isFalse();
    }

    @Test
    void entregaElUsuarioDeLaPeticion() throws Exception {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), "u@c.com", Set.of(Roles.CLIENTE), null, null);
        req.setAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO, usuario);

        Object resuelto = resolver.resolveArgument(parametro("requerido", UsuarioActual.class), null,
                new ServletWebRequest(req), null);

        assertThat(resuelto).isSameAs(usuario);
    }

    @Test
    void sinUsuarioYParametroRequeridoResponde401() throws Exception {
        MethodParameter requerido = parametro("requerido", UsuarioActual.class);
        ServletWebRequest peticion = new ServletWebRequest(req);

        assertThatThrownBy(() -> resolver.resolveArgument(requerido, null, peticion, null))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void sinUsuarioYParametroOpcionalDevuelveNulo() throws Exception {
        Object resuelto = resolver.resolveArgument(parametro("opcional", UsuarioActual.class), null,
                new ServletWebRequest(req), null);

        assertThat(resuelto).isNull();
    }
}
