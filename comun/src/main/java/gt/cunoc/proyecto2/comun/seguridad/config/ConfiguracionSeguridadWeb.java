package gt.cunoc.proyecto2.comun.seguridad.config;

import gt.cunoc.proyecto2.comun.seguridad.users.InterceptorRoles;
import gt.cunoc.proyecto2.comun.seguridad.users.ResolverUsuarioActual;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor de roles y el resolver de {@link UsuarioActual}. */
@Configuration
public class ConfiguracionSeguridadWeb implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new InterceptorRoles());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ResolverUsuarioActual());
    }
}
