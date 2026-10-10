package gt.cunoc.proyecto2.sales.configuracion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/*
 * Datos de la documentación Swagger (/swagger-ui.html) y botón "Authorize" para enviar el JWT.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sales Service",
                version = "1.0",
                description = "Asientos, venta de boletos en línea y en mostrador, pago con cartera y pago de alquileres"),
        security = @SecurityRequirement(name = ConfiguracionOpenApi.ESQUEMA_JWT))
@SecurityScheme(
        name = ConfiguracionOpenApi.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_JWT = "jwt";
}