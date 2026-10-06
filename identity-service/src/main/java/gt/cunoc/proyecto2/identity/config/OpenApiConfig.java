package gt.cunoc.proyecto2.identity.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Documentación OpenAPI del servicio Identity.
 */
@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI identityOpenApi() {

        return new OpenAPI().info(new Info()

                .title("Identity Service")

                .version("1.0")

                .description("Sesión, JWT, roles, perfiles, sucursales, tarifa de depreciación y cartera digital."));
    }

}
