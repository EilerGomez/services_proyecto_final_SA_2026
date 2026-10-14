package gt.cunoc.proyecto2.identity.services.oauth;

import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** El contexto OAuth levanta sin base de datos: cableado de beans + properties. */
class OAuthConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(OAuthConfig.class, GoogleOAuthProperties.class,
                    TokenOpacoService.class, CodigosUnSoloUso.class,
                    ClienteHttpGoogleRest.class, VerificadorGoogle.class)
            .withPropertyValues("app.oauth2.google.client-id=abc");

    @Test
    void cableadoOAuth() {
        contexto.run(ctx -> {
            assertThat(ctx).hasSingleBean(VerificadorGoogle.class);
            assertThat(ctx).hasSingleBean(CodigosUnSoloUso.class);
            assertThat(ctx).hasSingleBean(ClienteHttpGoogleRest.class);
            assertThat(ctx.getBean(GoogleOAuthProperties.class).getClientId()).isEqualTo("abc");
        });
    }
}
