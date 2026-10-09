package gt.cunoc.proyecto2.trips.configuracion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;


@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Trips Service",
                version = "1.0",
                description = "Rutas, viajes regulares y privados, alquileres, salida, llegada y costos"),
        security = @SecurityRequirement(name = ConfiguracionOpenApi.ESQUEMA_JWT))
@SecurityScheme(
        name = ConfiguracionOpenApi.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_JWT = "jwt";
}