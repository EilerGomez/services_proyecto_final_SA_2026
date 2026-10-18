package gt.cunoc.proyecto2.wallet.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void walletOpenApiExponeJwtYMismoOrigen() {
        var api = new OpenApiConfig().walletOpenApi();

        assertThat(api.getInfo().getTitle()).isEqualTo("Wallet Service");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getServers()).hasSize(1);
        assertThat(api.getServers().get(0).getUrl()).isEqualTo("/");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.ESQUEMA_JWT);
        assertThat(api.getSecurity()).hasSize(1);
    }
}
