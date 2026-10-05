package gt.cunoc.proyecto2.comun.seguridad.filtro;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.errores.EscritorErrores;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class FiltroAutenticacionTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ServicioJwt jwt = new ServicioJwt(json, "secreto-de-prueba-con-mas-de-32-caracteres",
            Duration.ofMinutes(60), Clock.systemUTC());
    private final FiltroAutenticacion filtro = new FiltroAutenticacion(jwt, new EscritorErrores(json));

    private final AtomicBoolean continuo = new AtomicBoolean(false);
    private final FilterChain cadena = (req, res) -> continuo.set(true);
    private final MockHttpServletRequest req = new MockHttpServletRequest("GET", "/fleet/buses");
    private final MockHttpServletResponse res = new MockHttpServletResponse();

    @Test
    void tokenValidoDejaElUsuarioEnLaPeticion() throws Exception {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), "a@b.com", Set.of(Roles.ADMIN_SUCURSAL),
                UUID.randomUUID(), null);
        req.addHeader("Authorization", "Bearer " + jwt.generar(usuario));

        filtro.doFilter(req, res, cadena);

        assertThat(continuo).isTrue();
        assertThat(req.getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO)).isEqualTo(usuario);
    }

    @Test
    void tokenInvalidoResponde401YNoContinua() throws Exception {
        req.addHeader("Authorization", "Bearer token-falso");

        filtro.doFilter(req, res, cadena);

        assertThat(continuo).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("TOKEN_INVALIDO");
    }

    @Test
    void sinTokenContinuaComoAnonimo() throws Exception {
        filtro.doFilter(req, res, cadena);

        assertThat(continuo).isTrue();
        assertThat(req.getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO)).isNull();
    }

    @Test
    void otroTipoDeAutorizacionSeIgnora() throws Exception {
        req.addHeader("Authorization", "Basic dXN1YXJpbzpjbGF2ZQ==");

        filtro.doFilter(req, res, cadena);

        assertThat(continuo).isTrue();
        assertThat(req.getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO)).isNull();
    }
}
