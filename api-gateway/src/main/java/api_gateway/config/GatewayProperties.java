package api_gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Agrupa las opciones gateway.* y Lombok genera sus getters y setters.
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private List<String> allowedOrigins = new ArrayList<>();
    private Security security = new Security();
    private Timeouts timeouts = new Timeouts();

    // Configuración de habilitación o deshabilitación de la autenticación JWT del gateway.
    @Getter
    @Setter
    public static class Security {
        private boolean enabled = true;
    }

    // Límites de espera para conectar y recibir la respuesta del microservicio destino.
    @Getter
    @Setter
    public static class Timeouts {
        private int connectTimeout = 3000;
        private Duration responseTimeout = Duration.ofSeconds(30);
    }
}
