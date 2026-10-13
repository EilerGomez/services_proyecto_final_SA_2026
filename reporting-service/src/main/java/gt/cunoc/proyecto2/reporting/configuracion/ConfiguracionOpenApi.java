package gt.cunoc.proyecto2.reporting.configuracion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Reporting Service",
                version = "1.0",
                description = "Modelo de lectura alimentado por eventos; reportes PDF y JSON"),
        // "/" = mismo origen: "Try it out" llama al host desde donde se abrió Swagger (el gateway en :8080
        // o el propio servicio), no a http://<servicio>:<puerto>, que el navegador no alcanza en Docker
        servers = @Server(url = "/", description = "Mismo origen (gateway o servicio)"),
        security = @SecurityRequirement(name = ConfiguracionOpenApi.ESQUEMA_JWT))
@SecurityScheme(
        name = ConfiguracionOpenApi.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_JWT = "jwt";
}