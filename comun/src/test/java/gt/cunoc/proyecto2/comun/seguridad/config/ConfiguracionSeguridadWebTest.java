package gt.cunoc.proyecto2.comun.seguridad.config;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.ResolverUsuarioActual;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

class ConfiguracionSeguridadWebTest {

    private final ConfiguracionSeguridadWeb configuracion = new ConfiguracionSeguridadWeb();

    @Test
    void registraElResolverDeUsuarioActual() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        configuracion.addArgumentResolvers(resolvers);

        assertThat(resolvers).singleElement().isInstanceOf(ResolverUsuarioActual.class);
    }

    @Test
    void registraElInterceptorDeRoles() {
        InterceptorRegistry registro = new InterceptorRegistry();

        configuracion.addInterceptors(registro);

        assertThat(registro).isNotNull();
    }
}
