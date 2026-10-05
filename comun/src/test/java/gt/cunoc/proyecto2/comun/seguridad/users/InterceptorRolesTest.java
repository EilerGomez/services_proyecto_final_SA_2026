package gt.cunoc.proyecto2.comun.seguridad.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.filtro.FiltroAutenticacion;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class InterceptorRolesTest {

    static class Controlador {
        @RequiereRol(Roles.ADMIN_SISTEMA)
        public void soloAdmin() {
        }

        @RequiereRol
        public void conSesion() {
        }

        public void publico() {
        }
    }

    @RequiereRol(Roles.CAJERO)
    static class ControladorCajero {
        public void vender() {
        }
    }

    private final InterceptorRoles interceptor = new InterceptorRoles();
    private final MockHttpServletRequest req = new MockHttpServletRequest();
    private final MockHttpServletResponse res = new MockHttpServletResponse();

    private HandlerMethod metodo(Object controlador, String nombre) throws Exception {
        return new HandlerMethod(controlador, controlador.getClass().getMethod(nombre));
    }

    private void conUsuario(String... roles) {
        req.setAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO,
                new UsuarioActual(UUID.randomUUID(), "u@correo.com", Set.of(roles), null, null));
    }

    @Test
    void recursosQueNoSonControladoresPasan() {
        assertThat(interceptor.preHandle(req, res, new Object())).isTrue();
    }

    @Test
    void metodoSinAnotacionEsPublico() throws Exception {
        assertThat(interceptor.preHandle(req, res, metodo(new Controlador(), "publico"))).isTrue();
    }

    @Test
    void sinUsuarioResponde401() throws Exception {
        HandlerMethod soloAdmin = metodo(new Controlador(), "soloAdmin");

        assertThatThrownBy(() -> interceptor.preHandle(req, res, soloAdmin))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void sinElRolResponde403() throws Exception {
        conUsuario(Roles.CLIENTE);
        HandlerMethod soloAdmin = metodo(new Controlador(), "soloAdmin");

        assertThatThrownBy(() -> interceptor.preHandle(req, res, soloAdmin))
                .isInstanceOf(ProhibidoException.class);
    }

    @Test
    void conElRolPasa() throws Exception {
        conUsuario(Roles.ADMIN_SISTEMA);
        assertThat(interceptor.preHandle(req, res, metodo(new Controlador(), "soloAdmin"))).isTrue();
    }

    @Test
    void anotacionSinRolesSoloExigeSesion() throws Exception {
        conUsuario(Roles.CLIENTE);
        assertThat(interceptor.preHandle(req, res, metodo(new Controlador(), "conSesion"))).isTrue();
    }

    @Test
    void anotacionEnLaClaseAplicaATodosSusMetodos() throws Exception {
        conUsuario(Roles.CLIENTE);
        HandlerMethod vender = metodo(new ControladorCajero(), "vender");
        assertThatThrownBy(() -> interceptor.preHandle(req, res, vender)).isInstanceOf(ProhibidoException.class);

        conUsuario(Roles.CAJERO);
        assertThat(interceptor.preHandle(req, res, vender)).isTrue();
    }
}
