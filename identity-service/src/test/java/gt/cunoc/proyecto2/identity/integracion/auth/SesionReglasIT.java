package gt.cunoc.proyecto2.identity.integracion.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Reglas de la sesión que no se ven en el camino feliz: los conflictos del registro, los estados de la
 * cuenta (bloqueo por intentos, bloqueo del administrador, inactiva), el doble factor completo con su
 * código, el cierre de sesión, el enlace de recuperación y el cambio de contraseña.
 *
 * <p>Los secretos no se leen de la base (se guardan con hash): el código del doble factor y el token
 * del enlace se toman del evento que Identity publica, que es lo mismo que recibe Notification.
 */
class SesionReglasIT extends PruebaIntegracion {

    private static final String ANA = "ana@pruebas.gt";
    private static final String NUEVA = "Nueva#2026";

    private ResponseEntity<String> login(String correo, String contrasena) {
        return post("/identity/auth/login", null,
                "{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena));
    }

    private ResponseEntity<String> verificar(String correo, String codigo) {
        return post("/identity/auth/login/verificar", null,
                "{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, codigo));
    }

    private UUID registrarAna() {
        return registrarCliente(ANA, "1000000-1", "1000000000101");
    }

    /** El código de 6 dígitos que Identity publicó para que Notification lo mande por correo. */
    private String codigoDobleFactor() {
        return JsonPath.read(esperarEventos("CodigoDobleFactorSolicitado").get("CodigoDobleFactorSolicitado"),
                "$.payload.codigo");
    }

    /** El token del enlace de recuperación, tal como va en el correo. */
    private String tokenDeRecuperacion() {
        String url = JsonPath.read(
                esperarEventos("RecuperacionContrasenaSolicitada").get("RecuperacionContrasenaSolicitada"),
                "$.payload.urlRecuperacion");
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private void fallarLogin(int veces) {
        for (int intento = 1; intento <= veces; intento++) {
            esError(login(ANA, "otra#2026"), 401, "CREDENCIALES_INVALIDAS");
        }
    }

    private String estadoDe(String correo) {
        return jdbc.queryForObject("SELECT estado FROM usuarios WHERE correo = ?", String.class, correo);
    }

    private void activarDobleFactor(String token) {
        assertThat(put("/identity/auth/doble-factor", token, "{\"activado\":true}")
                .getStatusCode().value()).isEqualTo(204);
    }

    // ----- Registro -----

    @Test
    void elCorreoElNitYElDpiNoSePuedenRepetir() {
        registrarAna();

        esError(post("/identity/auth/registro", null, """
                {"correo":"%s","contrasena":"%s","nombres":"Otra","apellidos":"Persona",
                 "nit":"2000000-2","dpi":"2000000000102","telefono":"55551234","direccion":"Xela"}
                """.formatted(ANA, CONTRASENA)), 409, "CORREO_EN_USO");
        esError(post("/identity/auth/registro", null, """
                {"correo":"beto@pruebas.gt","contrasena":"%s","nombres":"Beto","apellidos":"Lopez",
                 "nit":"1000000-1","dpi":"2000000000102","telefono":"55551234","direccion":"Xela"}
                """.formatted(CONTRASENA)), 409, "NIT_EN_USO");
        esError(post("/identity/auth/registro", null, """
                {"correo":"beto@pruebas.gt","contrasena":"%s","nombres":"Beto","apellidos":"Lopez",
                 "nit":"2000000-2","dpi":"1000000000101","telefono":"55551234","direccion":"Xela"}
                """.formatted(CONTRASENA)), 409, "DPI_EN_USO");
        assertThat(contar("usuarios")).isEqualTo(2);        // Ana y el admin de la prueba
    }

    // ----- Estados de la cuenta -----

    @Test
    void unCorreoDesconocidoYUnaCuentaDeGoogleSinClaveNoEntranPorContrasena() {
        registrarAna();

        // Nadie con ese correo: no hay a quién contarle el intento fallido
        esError(login("nadie@pruebas.gt", CONTRASENA), 401, "CREDENCIALES_INVALIDAS");
        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuarios WHERE correo = ?",
                Integer.class, ANA)).isZero();

        // Cuenta sin contraseña local (como las de Google): tampoco entra, pero sí le cuenta el intento
        jdbc.update("UPDATE usuarios SET contrasena_hash = NULL WHERE correo = ?", ANA);
        esError(login(ANA, CONTRASENA), 401, "CREDENCIALES_INVALIDAS");
        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuarios WHERE correo = ?",
                Integer.class, ANA)).isEqualTo(1);
    }

    @Test
    void elBloqueoPorIntentosVenceYLaCuentaSeReactivaSola() {
        registrarAna();
        fallarLogin(5);
        assertThat(estadoDe(ANA)).isEqualTo("BLOQUEADO");

        // Pasados los 15 minutos del bloqueo temporal
        jdbc.update("UPDATE usuarios SET bloqueado_hasta = now() - interval '1 minute' WHERE correo = ?", ANA);

        assertThat(login(ANA, CONTRASENA).getStatusCode().value()).isEqualTo(200);
        assertThat(estadoDe(ANA)).isEqualTo("ACTIVO");
        assertThat(jdbc.queryForObject("SELECT bloqueado_hasta FROM usuarios WHERE correo = ?",
                java.sql.Timestamp.class, ANA)).isNull();
    }

    @Test
    void elBloqueoDelAdministradorNoVenceNiSeVuelveTemporal() {
        registrarAna();
        // BLOQUEADO sin fecha = lo puso un administrador
        jdbc.update("UPDATE usuarios SET estado = 'BLOQUEADO', bloqueado_hasta = NULL WHERE correo = ?", ANA);

        esError(login(ANA, CONTRASENA), 401, "CUENTA_BLOQUEADA");
        fallarLogin(5);

        // Los intentos fallidos no lo convierten en un bloqueo con vencimiento
        assertThat(estadoDe(ANA)).isEqualTo("BLOQUEADO");
        assertThat(jdbc.queryForObject("SELECT bloqueado_hasta FROM usuarios WHERE correo = ?",
                java.sql.Timestamp.class, ANA)).isNull();
        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuarios WHERE correo = ?",
                Integer.class, ANA)).isZero();
    }

    @Test
    void unaCuentaInactivaNoEntraAunqueLaContrasenaSeaCorrecta() {
        registrarAna();
        jdbc.update("UPDATE usuarios SET estado = 'INACTIVO' WHERE correo = ?", ANA);

        esError(login(ANA, CONTRASENA), 401, "CUENTA_INACTIVA");
    }

    // ----- Doble factor -----

    @Test
    void conElCodigoDelCorreoSeCompletaElLoginYNoSirveDosVeces() {
        registrarAna();
        activarDobleFactor(PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenAcceso"));

        assertThat(login(ANA, CONTRASENA).getStatusCode().value()).isEqualTo(202);
        String codigo = codigoDobleFactor();

        ResponseEntity<String> sesion = verificar(ANA, codigo);

        assertThat(sesion.getStatusCode().value()).as(sesion.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(sesion, "$.tokenAcceso")).isNotBlank();
        // El desafío quedó usado: el mismo código ya no vale
        esError(verificar(ANA, codigo), 401, "DOBLE_FACTOR_INVALIDO");
    }

    @Test
    void unCodigoVencidoPideIniciarSesionDeNuevo() {
        registrarAna();
        activarDobleFactor(PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenAcceso"));
        assertThat(login(ANA, CONTRASENA).getStatusCode().value()).isEqualTo(202);
        String codigo = codigoDobleFactor();

        jdbc.update("UPDATE codigos_doble_factor SET expira_en = now() - interval '1 minute'");

        esError(verificar(ANA, codigo), 401, "DOBLE_FACTOR_VENCIDO");
    }

    @Test
    void cincoCodigosEquivocadosBloqueanLaCuenta() {
        registrarAna();
        activarDobleFactor(PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenAcceso"));
        assertThat(login(ANA, CONTRASENA).getStatusCode().value()).isEqualTo(202);
        String codigo = codigoDobleFactor();
        String equivocado = codigo.equals("000000") ? "111111" : "000000";

        for (int intento = 1; intento <= 5; intento++) {
            esError(verificar(ANA, equivocado), 401, "DOBLE_FACTOR_INVALIDO");
        }

        assertThat(estadoDe(ANA)).isEqualTo("BLOQUEADO");
        assertThat(jdbc.queryForObject("SELECT intentos FROM codigos_doble_factor", Integer.class)).isEqualTo(5);
        // Con la cuenta bloqueada ni el código bueno sirve
        esError(verificar(ANA, codigo), 401, "DOBLE_FACTOR_INVALIDO");
        esError(login(ANA, CONTRASENA), 401, "CUENTA_BLOQUEADA");
    }

    @Test
    void sinDobleFactorActivoOSinDesafioPendienteElCodigoNoVale() {
        registrarAna();

        // Correo que no existe y cuenta con el doble factor apagado
        esError(verificar("nadie@pruebas.gt", "123456"), 401, "DOBLE_FACTOR_INVALIDO");
        esError(verificar(ANA, "123456"), 401, "DOBLE_FACTOR_INVALIDO");

        // Activado, pero sin haber iniciado sesión: no hay desafío que verificar
        String token = PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenAcceso");
        activarDobleFactor(token);
        esError(verificar(ANA, "123456"), 401, "DOBLE_FACTOR_INVALIDO");

        // Y apagarlo vuelve a dejar el login en un solo paso
        assertThat(put("/identity/auth/doble-factor", token, "{\"activado\":false}")
                .getStatusCode().value()).isEqualTo(204);
        assertThat(login(ANA, CONTRASENA).getStatusCode().value()).isEqualTo(200);
    }

    // ----- Cerrar sesión -----

    @Test
    void cerrarSesionRevocaSoloElTokenPropioYUnaSolaVez() {
        registrarAna();
        ResponseEntity<String> sesionAna = login(ANA, CONTRASENA);
        String tokenAna = PruebaIntegracion.json(sesionAna, "$.tokenAcceso");
        String refrescoAna = PruebaIntegracion.json(sesionAna, "$.tokenRefresco");
        UUID betoId = registrarCliente("beto@pruebas.gt", "2000000-2", "2000000000102");
        String refrescoBeto = PruebaIntegracion.json(login("beto@pruebas.gt", CONTRASENA), "$.tokenRefresco");

        // Un token que no existe y el de otra persona no se revocan
        assertThat(post("/identity/auth/cerrar-sesion", tokenAna, "{\"tokenRefresco\":\"inexistente\"}")
                .getStatusCode().value()).isEqualTo(204);
        assertThat(post("/identity/auth/cerrar-sesion", tokenAna,
                "{\"tokenRefresco\":\"%s\"}".formatted(refrescoBeto)).getStatusCode().value()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tokens_refresco WHERE revocado_en IS NOT NULL",
                Integer.class)).isZero();

        // El propio sí, y repetirlo no cambia nada
        assertThat(post("/identity/auth/cerrar-sesion", tokenAna,
                "{\"tokenRefresco\":\"%s\"}".formatted(refrescoAna)).getStatusCode().value()).isEqualTo(204);
        assertThat(post("/identity/auth/cerrar-sesion", tokenAna,
                "{\"tokenRefresco\":\"%s\"}".formatted(refrescoAna)).getStatusCode().value()).isEqualTo(204);

        esError(post("/identity/auth/refrescar", null,
                "{\"tokenRefresco\":\"%s\"}".formatted(refrescoAna)), 401, "TOKEN_REFRESCO_INVALIDO");
        assertThat(betoId).isNotNull();
    }

    @Test
    void unaSesionDeUnaCuentaInactivaNoSeRenueva() {
        registrarAna();
        String refresco = PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenRefresco");
        jdbc.update("UPDATE usuarios SET estado = 'INACTIVO' WHERE correo = ?", ANA);

        esError(post("/identity/auth/refrescar", null,
                "{\"tokenRefresco\":\"%s\"}".formatted(refresco)), 401, "CUENTA_INACTIVA");
    }

    // ----- Recuperación -----

    @Test
    void elEnlaceDelCorreoCambiaLaContrasenaYCierraLasSesiones() {
        registrarAna();
        String refresco = PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenRefresco");
        assertThat(post("/identity/auth/recuperar", null, "{\"correo\":\"%s\"}".formatted(ANA))
                .getStatusCode().value()).isEqualTo(202);
        String token = tokenDeRecuperacion();

        assertThat(post("/identity/auth/restablecer", null,
                "{\"token\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(token, NUEVA))
                .getStatusCode().value()).isEqualTo(204);

        assertThat(login(ANA, NUEVA).getStatusCode().value()).isEqualTo(200);
        esError(login(ANA, CONTRASENA), 401, "CREDENCIALES_INVALIDAS");
        // Las sesiones abiertas se cierran al cambiar la contraseña
        esError(post("/identity/auth/refrescar", null,
                "{\"tokenRefresco\":\"%s\"}".formatted(refresco)), 401, "TOKEN_REFRESCO_INVALIDO");
        // Y el enlace ya no sirve otra vez
        esError(post("/identity/auth/restablecer", null,
                "{\"token\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(token, NUEVA)), 422, "TOKEN_USADO");
    }

    @Test
    void unEnlaceVencidoOInventadoNoCambiaNada() {
        registrarAna();
        post("/identity/auth/recuperar", null, "{\"correo\":\"%s\"}".formatted(ANA));
        String token = tokenDeRecuperacion();
        jdbc.update("UPDATE tokens_recuperacion SET expira_en = now() - interval '1 minute'");

        esError(post("/identity/auth/restablecer", null,
                "{\"token\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(token, NUEVA)), 422, "TOKEN_VENCIDO");
        esError(post("/identity/auth/restablecer", null,
                "{\"token\":\"inventado\",\"nuevaContrasena\":\"%s\"}".formatted(NUEVA)), 404, "TOKEN_NO_EXISTE");
        assertThat(login(ANA, CONTRASENA).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void recuperarUnCorreoQueNoExisteOInactivoNoDiceNadaNiPublica() {
        registrarAna();
        jdbc.update("UPDATE usuarios SET estado = 'INACTIVO' WHERE correo = ?", ANA);

        // Siempre 202: la respuesta no revela si el correo existe o si la cuenta está activa
        assertThat(post("/identity/auth/recuperar", null, "{\"correo\":\"nadie@pruebas.gt\"}")
                .getStatusCode().value()).isEqualTo(202);
        assertThat(post("/identity/auth/recuperar", null, "{\"correo\":\"%s\"}".formatted(ANA))
                .getStatusCode().value()).isEqualTo(202);

        assertThat(contar("tokens_recuperacion")).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM eventos_salida WHERE tipo_evento = 'RecuperacionContrasenaSolicitada'""",
                Integer.class)).isZero();
    }

    @Test
    void restablecerDesbloqueaLaCuentaQueSeBloqueoPorIntentos() {
        registrarAna();
        // El enlace se pide con la cuenta activa y el bloqueo llega después
        post("/identity/auth/recuperar", null, "{\"correo\":\"%s\"}".formatted(ANA));
        String token = tokenDeRecuperacion();
        fallarLogin(5);
        assertThat(estadoDe(ANA)).isEqualTo("BLOQUEADO");

        assertThat(post("/identity/auth/restablecer", null,
                "{\"token\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(token, NUEVA))
                .getStatusCode().value()).isEqualTo(204);

        assertThat(estadoDe(ANA)).isEqualTo("ACTIVO");
        assertThat(login(ANA, NUEVA).getStatusCode().value()).isEqualTo(200);
    }

    // ----- Cambio de contraseña -----

    @Test
    void cambiarLaContrasenaPropiaExigeLaActual() {
        registrarAna();
        String token = PruebaIntegracion.json(login(ANA, CONTRASENA), "$.tokenAcceso");

        esError(put("/identity/auth/contrasena", token,
                "{\"contrasenaActual\":\"otra#2026\",\"nuevaContrasena\":\"%s\"}".formatted(NUEVA)),
                422, "CONTRASENA_ACTUAL_INCORRECTA");

        assertThat(put("/identity/auth/contrasena", token,
                "{\"contrasenaActual\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(CONTRASENA, NUEVA))
                .getStatusCode().value()).isEqualTo(204);
        assertThat(login(ANA, NUEVA).getStatusCode().value()).isEqualTo(200);

        // Una cuenta de Google no tiene contraseña actual que comparar
        jdbc.update("UPDATE usuarios SET contrasena_hash = NULL WHERE correo = ?", ANA);
        esError(put("/identity/auth/contrasena", token,
                "{\"contrasenaActual\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(NUEVA, CONTRASENA)),
                422, "CONTRASENA_ACTUAL_INCORRECTA");
    }
}
