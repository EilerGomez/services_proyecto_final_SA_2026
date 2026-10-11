package gt.cunoc.proyecto2.maintenance.configuracion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Maintenance Service",
                version = "1.0",
                description = "Categorías de gasto y servicios de taller (mano de obra y repuestos)"),
        security = @SecurityRequirement(name = ConfiguracionOpenApi.ESQUEMA_JWT))
@SecurityScheme(
        name = ConfiguracionOpenApi.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_JWT = "jwt";
}
