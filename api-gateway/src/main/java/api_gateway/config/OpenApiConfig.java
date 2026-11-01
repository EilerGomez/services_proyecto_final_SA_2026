package api_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Metadatos de la documentación OpenAPI que describe el propio gateway.
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI gatewayOpenApi() {
        return new OpenAPI().info(new Info().title("API Gateway")
                .version("1.0").description("Gateway de identity, fleet, trips, sales, "
                        + "maintenance, reporting, notification y wallet. "
                        + "Selecciona el microservicio en Swagger UI para ver sus operaciones."));
    }
}
