package api_gateway.config;

import api_gateway.constants.HeaderNames;
import api_gateway.exception.ErrorResponseWriter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * Configura CORS para los navegadores y adapta sus rechazos al formato de error del gateway.
 * Las llamadas entre microservicios no dependen de CORS, esta configuración controla solicitudes
 * de frontend que incluyen el origen del navegador.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayProperties.class)
public class CorsConfig {
    /**
     * Registra las reglas CORS globales que Spring Security aplica a las solicitudes de navegador.
     * Los orígenes proceden de GatewayProperties, el frontend puede leer los headers de correlación
     * y de descarga de documentos, como Content-Disposition.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewayProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.getAllowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        // Content-Disposition: el frontend lee el nombre del PDF que descarga de reporting.
        cors.setExposedHeaders(List.of(HeaderNames.CORRELATION_ID, HttpHeaders.CONTENT_DISPOSITION));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /**
     * Agrega el cuerpo JSON común cuando Spring rechaza CORS con 403 antes del manejador de
     * autorización y la respuesta todavía no se ha confirmado.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    WebFilter corsRejectionJsonFilter(ErrorResponseWriter writer) {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.defer(() -> {
            if (!exchange.getResponse().isCommitted()
                    && HttpStatus.FORBIDDEN.equals(exchange.getResponse().getStatusCode())) {
                return writer.write(exchange, HttpStatus.FORBIDDEN);
            }
            return Mono.empty();
        }));
    }
}
