package gt.cunoc.proyecto2.identity.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** OpenApiConfig: clase sin cubrir. */
class OpenApiConfigTest {

    @Test
    void identityOpenApiExponeJwtYMismoOrigen() {
        var api = new OpenApiConfig().identityOpenApi();

        assertThat(api.getInfo().getTitle()).isEqualTo("Identity Service");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getServers()).hasSize(1);
        assertThat(api.getServers().get(0).getUrl()).isEqualTo("/");
        assertThat(api.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.ESQUEMA_JWT);
        assertThat(api.getSecurity()).hasSize(1);
    }
}
