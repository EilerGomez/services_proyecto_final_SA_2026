package gt.cunoc.proyecto2.reporting.configuracion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configura la documentación OpenAPI/Swagger del servicio de reportes.
 *
 * <p>Declara el título, la versión, el servidor y el esquema de seguridad JWT con el que Swagger
 * autoriza las peticiones desde su interfaz.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Reporting Service",
        version = "1.0",
        description = "Modelo de lectura alimentado por eventos; reportes PDF y JSON"),
        servers = @Server(url = "/", description = "Mismo origen (gateway o servicio)"),
        security = @SecurityRequirement(name = ConfiguracionOpenApi.ESQUEMA_JWT))
@SecurityScheme(
        name = ConfiguracionOpenApi.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class ConfiguracionOpenApi {

    // Nombre interno del esquema de seguridad que Swagger muestra como botón "Authorize".
    public static final String ESQUEMA_JWT = "jwt";
}