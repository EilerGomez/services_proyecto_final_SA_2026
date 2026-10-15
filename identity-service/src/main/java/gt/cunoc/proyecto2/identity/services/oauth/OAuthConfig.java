package gt.cunoc.proyecto2.identity.services.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Bean explícito: Boot 4 no auto-configura el builder de RestClient. */
@Configuration(proxyBeanMethods = false)
public class OAuthConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
