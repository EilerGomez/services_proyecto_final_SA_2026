package gt.cunoc.proyecto2.notification.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Documentación OpenAPI del servicio Notification.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Notification Service")
                .version("1.0")
                .description("Correos y recordatorios alimentados por eventos."));
    }
}
