package gt.cunoc.proyecto2.identity.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Documentación OpenAPI del servicio Identity.
 */
@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad: habilita el botón "Authorize" para pegar el JWT. */
    public static final String ESQUEMA_JWT = "jwt";


    @Bean
    public OpenAPI identityOpenApi() {

        return new OpenAPI().info(new Info()

                .title("Identity Service")

                .version("1.0")

                .description("Sesión, JWT, roles, perfiles, sucursales, tarifa de depreciación y cartera digital."))
                // "/" = mismo origen: "Try it out" llama al gateway (o al propio servicio), no al host interno de Docker
                .servers(List.of(new Server().url("/").description("Mismo origen (gateway o servicio)")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }

}