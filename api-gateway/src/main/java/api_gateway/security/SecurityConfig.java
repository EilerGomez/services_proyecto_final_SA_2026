package api_gateway.security;

import api_gateway.constants.PublicEndpoints;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
// Own decoder lifecycle: even configured JWT properties must not activate JWT when security is off.
@EnableAutoConfiguration(exclude = ReactiveOAuth2ResourceServerAutoConfiguration.class)
public class SecurityConfig {
    private static ServerHttpSecurity base(ServerHttpSecurity http,
            JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) {
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

    @Bean
    @ConditionalOnProperty(prefix = "gateway.security", name = "enabled", havingValue = "false", matchIfMissing = true)
    SecurityWebFilterChain publicSecurity(ServerHttpSecurity http,
            JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) {
        return base(http, entryPoint, deniedHandler)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll()).build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gateway.security", name = "enabled", havingValue = "true")
    static class JwtSecurity {
        @Bean
        SecurityWebFilterChain jwtSecurity(ServerHttpSecurity http, ReactiveJwtDecoder decoder,
                JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) {
            return base(http, entryPoint, deniedHandler)
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers(HttpMethod.OPTIONS).permitAll()
                            .pathMatchers(PublicEndpoints.PATHS).permitAll()
                            .anyExchange().authenticated())
                    .oauth2ResourceServer(resourceServer -> resourceServer
                            .jwt(jwt -> jwt.jwtDecoder(decoder))
                            .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                    .build();
        }
    }
}
