package api_gateway.security;

import api_gateway.constants.PublicEndpoints;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

/**
 * Define las cadenas reactivas de seguridad que protegen las solicitudes del gateway.
 *
 * Con seguridad habilitada, las rutas públicas continúan anónimas y las demás requieren un JWT,
 * la opción de desarrollo deshabilita la autenticación del borde, pero no cambia el enrutamiento.
 *
 * Se excluye la autoconfiguración JWT para controlar cuándo se crea el decoder.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableAutoConfiguration(exclude = ReactiveOAuth2ResourceServerAutoConfiguration.class)
public class SecurityConfig {

    // Construye la base común, sin autenticación por formulario, con CORS y con manejadores JSON uniformes.
    private static ServerHttpSecurity base(ServerHttpSecurity http, JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .cors(Customizer.withDefaults())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler));
    }

    // Permite todas las solicitudes en Spring Security, no desactiva el ruteo ni el bloqueo independiente de las rutas internas.
    @Bean
    @ConditionalOnProperty(prefix = "gateway.security", name = "enabled", havingValue = "false")
    SecurityWebFilterChain publicSecurity(ServerHttpSecurity http, JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) {
        return base(http, entryPoint, deniedHandler)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll()).build();
    }

    // Configuración JWT activada por defecto, incluso cuando la propiedad no está definida.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gateway.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class JwtSecurity {

        // Permite las rutas públicas y exige una autenticación JWT válida en las demás.
        @Bean
        SecurityWebFilterChain jwtSecurity(ServerHttpSecurity http, ReactiveJwtDecoder decoder, JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) {
            ServerWebExchangeMatcher rutasPublicas = PublicEndpoints.matcher();
            return base(http, entryPoint, deniedHandler)
                    .authorizeExchange(exchanges -> exchanges
                            .matchers(rutasPublicas).permitAll()
                            .anyExchange().authenticated())
                    .oauth2ResourceServer(resourceServer -> resourceServer
                            .bearerTokenConverter(new RutasPublicasBearerTokenConverter(rutasPublicas))
                            .jwt(jwt -> jwt.jwtDecoder(decoder))
                            .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                    .build();
        }
    }
}
