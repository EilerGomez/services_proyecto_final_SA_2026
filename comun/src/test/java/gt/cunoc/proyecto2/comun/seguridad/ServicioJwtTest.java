package gt.cunoc.proyecto2.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ServicioJwtTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";
    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");

    private final JsonMapper json = JsonMapper.builder().build();

    private ServicioJwt servicio(Instant instante) {
        return new ServicioJwt(json, SECRETO, Duration.ofMinutes(60), Clock.fixed(instante, ZoneOffset.UTC));
    }

    private final UsuarioActual usuario = new UsuarioActual(
            UUID.randomUUID(), "admin@correo.com",
            Set.of(Roles.ADMIN_SUCURSAL, Roles.CLIENTE), UUID.randomUUID(), null);

    @Test
    void generaYValidaElMismoUsuario() {
        String token = servicio(AHORA).generar(usuario);

        UsuarioActual leido = servicio(AHORA.plusSeconds(60)).validar(token);

        assertThat(leido).isEqualTo(usuario);
    }

    @Test
    void rechazaTokenAlterado() {
        String token = servicio(AHORA).generar(usuario);
        String[] partes = token.split("\\.");
        String alterado = partes[0] + "." + partes[1] + "x." + partes[2];

        assertThatThrownBy(() -> servicio(AHORA).validar(alterado))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        ServicioJwt otro = new ServicioJwt(json, "otro-secreto-distinto-con-mas-de-32-caracteres",
                Duration.ofMinutes(60), Clock.fixed(AHORA, ZoneOffset.UTC));
        String token = otro.generar(usuario);

        assertThatThrownBy(() -> servicio(AHORA).validar(token))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void rechazaTokenExpirado() {
        String token = servicio(AHORA).generar(usuario);

        assertThatThrownBy(() -> servicio(AHORA.plus(Duration.ofMinutes(61))).validar(token))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("TOKEN_EXPIRADO");
    }

    @Test
    void rechazaTextoQueNoEsToken() {
        assertThatThrownBy(() -> servicio(AHORA).validar("esto-no-es-un-jwt"))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void exigeSecretoDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> new ServicioJwt(json, "corto", Duration.ofMinutes(60), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
