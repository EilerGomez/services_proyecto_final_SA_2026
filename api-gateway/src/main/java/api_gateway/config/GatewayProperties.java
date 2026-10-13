package api_gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private List<String> allowedOrigins = new ArrayList<>();
    private Security security = new Security();
    private Timeouts timeouts = new Timeouts();

    @Getter
    @Setter
    public static class Security {
        /** Activada por defecto; GATEWAY_SECURITY_ENABLED=false la apaga (solo desarrollo). */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Timeouts {
        private int connectTimeout = 3000;
        private Duration responseTimeout = Duration.ofSeconds(30);
    }
}
