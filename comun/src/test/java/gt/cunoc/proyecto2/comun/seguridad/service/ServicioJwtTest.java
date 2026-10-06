package gt.cunoc.proyecto2.comun.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ServicioJwtTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";
    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final String CABECERA_HS256 = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

    private final JsonMapper json = JsonMapper.builder().build();

    private final UsuarioActual usuario = new UsuarioActual(
            UUID.randomUUID(), "admin@correo.com",
            Set.of(Roles.ADMIN_SUCURSAL, Roles.CLIENTE), UUID.randomUUID(), null);

    private ServicioJwt servicio(Instant instante) {
        return new ServicioJwt(json, SECRETO, Duration.ofMinutes(60), Clock.fixed(instante, ZoneOffset.UTC));
    }

    private static String b64(String texto) {
        return B64.encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    /** Arma un token firmado correctamente con contenido arbitrario (para probar claims inválidos). */
    private static String firmado(String contenidoJson) throws Exception {
        String datos = CABECERA_HS256 + "." + b64(contenidoJson);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return datos + "." + B64.encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void generaYValidaElMismoUsuario() {
        String token = servicio(AHORA).generar(usuario);

        UsuarioActual leido = servicio(AHORA.plusSeconds(60)).validar(token);

        assertThat(leido).isEqualTo(usuario);
    }

    @Test
    void conservaElIdDeChofer() {
        UsuarioActual chofer = new UsuarioActual(UUID.randomUUID(), "chofer@correo.com",
                Set.of(Roles.CHOFER), null, UUID.randomUUID());

        UsuarioActual leido = servicio(AHORA).validar(servicio(AHORA).generar(chofer));

        assertThat(leido.choferId()).isEqualTo(chofer.choferId());
        assertThat(leido.sucursalId()).isNull();
    }

    @Test
    void elConstructorDeSpringUsaLaDuracionEnMinutos() {
        ServicioJwt deSpring = new ServicioJwt(json, SECRETO, 60L);

        assertThat(deSpring.validar(deSpring.generar(usuario))).isEqualTo(usuario);
    }

    @Test
    void rechazaTokenAlterado() {
        String[] partes = servicio(AHORA).generar(usuario).split("\\.");
        String alterado = partes[0] + "." + partes[1] + "x." + partes[2];

        assertThatThrownBy(() -> servicio(AHORA).validar(alterado))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        ServicioJwt otro = new ServicioJwt(json, "otro-secreto-distinto-con-mas-de-32-caracteres",
                Duration.ofMinutes(60), Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> servicio(AHORA).validar(otro.generar(usuario)))
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
    void rechazaAlgoritmoNone() {
        String token = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64("{\"sub\":\"x\"}") + ".firma";

        assertThatThrownBy(() -> servicio(AHORA).validar(token))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void rechazaTextoQueNoEsToken() {
        assertThatThrownBy(() -> servicio(AHORA).validar("esto-no-es-un-jwt"))
                .isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> servicio(AHORA).validar("@@@.@@@.@@@"))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void rechazaTokenSinExpiracion() throws Exception {
        String token = firmado("{\"sub\":\"" + UUID.randomUUID() + "\"}");

        assertThatThrownBy(() -> servicio(AHORA).validar(token))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("TOKEN_EXPIRADO");
    }

    @Test
    void rechazaIdDeUsuarioInvalido() throws Exception {
        long exp = AHORA.getEpochSecond() + 3600;
        String token = firmado("{\"sub\":\"no-es-uuid\",\"exp\":" + exp + "}");

        assertThatThrownBy(() -> servicio(AHORA).validar(token))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void rolesConFormatoInvalidoQuedanVacios() throws Exception {
        long exp = AHORA.getEpochSecond() + 3600;
        UUID id = UUID.randomUUID();
        String token = firmado("{\"sub\":\"" + id + "\",\"correo\":\"a@b.com\",\"roles\":\"ADMIN\",\"exp\":" + exp + "}");

        UsuarioActual leido = servicio(AHORA).validar(token);

        assertThat(leido.usuarioId()).isEqualTo(id);
        assertThat(leido.roles()).isEmpty();
    }

    @Test
    void exigeSecretoDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> new ServicioJwt(json, "corto", Duration.ofMinutes(60), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ServicioJwt(json, null, Duration.ofMinutes(60), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tokenDeServicioLlevaSoloElRolServicio() {
        UsuarioActual leido = servicio(AHORA).validar(servicio(AHORA).generarServicio("fleet-service"));

        assertThat(leido.roles()).containsExactly(Roles.SERVICIO);
        assertThat(leido.esServicio()).isTrue();
        assertThat(leido.correo()).isEqualTo("fleet-service");
        assertThat(leido.sucursalId()).isNull();
        assertThat(leido.choferId()).isNull();
    }

    @Test
    void elIdDeServicioEsFijoPorNombre() {
        UsuarioActual leido = servicio(AHORA).validar(servicio(AHORA).generarServicio("fleet-service"));

        assertThat(leido.usuarioId()).isEqualTo(ServicioJwt.idServicio("fleet-service"));
        assertThat(ServicioJwt.idServicio("fleet-service")).isEqualTo(ServicioJwt.idServicio("fleet-service"));
        assertThat(ServicioJwt.idServicio("fleet-service")).isNotEqualTo(ServicioJwt.idServicio("trips-service"));
    }

    @Test
    void tokenDeServicioVenceALosCincoMinutosAunqueLaDuracionConfiguradaSeaMayor() {
        String token = servicio(AHORA).generarServicio("trips-service");

        assertThat(servicio(AHORA.plus(Duration.ofMinutes(4))).validar(token).esServicio()).isTrue();
        assertThatThrownBy(() -> servicio(AHORA.plus(ServicioJwt.DURACION_TOKEN_SERVICIO)).validar(token))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("TOKEN_EXPIRADO");
    }

    @Test
    void tokenDeServicioExigeNombre() {
        assertThatThrownBy(() -> servicio(AHORA).generarServicio(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio(AHORA).generarServicio("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}