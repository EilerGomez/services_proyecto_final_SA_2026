package gt.cunoc.proyecto2.identity.services.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.auth.CanjeTicketRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.DesafioDobleFactor;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.ResultadoInicioSesion;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.UrlGoogleResponse;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.CodigoDobleFactorRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.IdentidadUsuarioFactory;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import gt.cunoc.proyecto2.identity.services.oauth.CodigosUnSoloUso;
import gt.cunoc.proyecto2.identity.services.oauth.ClienteHttpGoogle;
import gt.cunoc.proyecto2.identity.services.oauth.GoogleOAuthProperties;
import gt.cunoc.proyecto2.identity.services.oauth.VerificadorGoogle;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Login con Google: states, tickets, firma del id_token, enlace y creación de cuentas. */
@ExtendWith(MockitoExtension.class)
class OAuthGoogleTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final String CLIENTE_ID = "google-client-test";

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;
    @Mock private CarteraRepository carteras;
    @Mock private TokenRefrescoRepository refrescos;
    @Mock private TokenRecuperacionRepository recuperaciones;
    @Mock private CodigoDobleFactorRepository desafios;
    @Mock private ServicioJwt jwt;
    @Mock private UsuarioEventPublisher publicador;
    @Mock private UserService servicioUsuarios;
    @Mock private VigilanciaAcceso vigilancia;
    @Mock private ClienteHttpGoogle httpGoogle;
    @Mock private VerificadorGoogle google;

    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();
    @Spy private TokenOpacoService opacos = new TokenOpacoService();

    private GoogleOAuthProperties oauth;
    private AuthServiceImpl servicio;

    @BeforeEach
    void preparar() {
        oauth = new GoogleOAuthProperties();
        oauth.setClientId(CLIENTE_ID);
        oauth.setClientSecret("secreto-test");
        oauth.setRedirectUri("http://localhost:8080/identity/auth/oauth2/google/callback");
        oauth.setFrontendUrl("http://localhost:4200");
        servicio = new AuthServiceImpl(usuarios, perfiles, roles, carteras, refrescos,
                recuperaciones, desafios, claves, jwt, 60L, new IdentidadUsuarioFactory(),
                opacos, vigilancia, publicador, servicioUsuarios, "http://localhost:5173",
                oauth, new CodigosUnSoloUso(opacos), google);
    }

    // ---------- url ----------

    @Test
    void urlSinConfigResponde503() {
        oauth.setClientId("");
        ServicioNoDisponibleException error = assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.urlGoogle());
        assertEquals("OAUTH_NO_CONFIGURADO", error.getCodigo());
    }

    @Test
    void urlContieneClienteRedirectYStateDeUnSoloUso() {
        UrlGoogleResponse respuesta = servicio.urlGoogle();
        assertTrue(respuesta.url().contains("accounts.google.com"));
        assertTrue(respuesta.url().contains("client_id=" + CLIENTE_ID));
        assertTrue(respuesta.url().contains("state=" + respuesta.state()));
        // El state se consume una sola vez en el callback.
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta("sub-1", "nueva@correo.com"));
        stubCrear("nueva@correo.com");
        String destino = servicio.callbackGoogle("code", respuesta.state());
        assertTrue(destino.contains("?ticket="));
        assertTrue(servicio.callbackGoogle("code", respuesta.state()).contains("OAUTH_ESTADO_INVALIDO"));
    }

    // ---------- callback: errores ----------

    @Test
    void callbackSinConfigRedirigeConError() {
        oauth.setClientId("");
        assertTrue(servicio.callbackGoogle("code", "state").contains("OAUTH_NO_CONFIGURADO"));
    }

    @Test
    void callbackSinStateRedirigeConError() {
        assertTrue(servicio.callbackGoogle("code", "inexistente").contains("OAUTH_ESTADO_INVALIDO"));
        assertTrue(servicio.callbackGoogle("code", null).contains("OAUTH_ESTADO_INVALIDO"));
    }

    @Test
    void callbackConStateVencidoRedirigeConError() {
        oauth.setStateTtlSegundos(-1);
        String estado = servicio.urlGoogle().state();
        assertTrue(servicio.callbackGoogle("code", estado).contains("OAUTH_ESTADO_INVALIDO"));
    }

    @Test
    void callbackSinCodigoRedirigeConError() {
        String estado = servicio.urlGoogle().state();
        assertTrue(servicio.callbackGoogle(null, estado).contains("OAUTH_CODIGO_INVALIDO"));
    }

    @Test
    void callbackSiGoogleFallaRedirigeConSuCodigo() {
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("mal")).thenThrow(
                new ServicioNoDisponibleException("GOOGLE_SIN_RESPUESTA", "x"));
        assertTrue(servicio.callbackGoogle("mal", estado).contains("GOOGLE_SIN_RESPUESTA"));
    }

    @Test
    void callbackConCorreoNoVerificadoNoAbreSesion() {
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(
                new VerificadorGoogle.CuentaGoogle("sub-1", "nueva@correo.com", false, "Ana", null));
        assertTrue(servicio.callbackGoogle("code", estado).contains("OAUTH_CORREO_NO_VERIFICADO"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void callbackConCorreoVacioONuloNoAbreSesion() {
        String e1 = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("c1")).thenReturn("t1");
        when(google.verificar("t1")).thenReturn(
                new VerificadorGoogle.CuentaGoogle("s1", "", true, "Ana", null));
        assertTrue(servicio.callbackGoogle("c1", e1).contains("OAUTH_CORREO_NO_VERIFICADO"));
        String e2 = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("c2")).thenReturn("t2");
        when(google.verificar("t2")).thenReturn(
                new VerificadorGoogle.CuentaGoogle("s2", null, true, "Ana", null));
        assertTrue(servicio.callbackGoogle("c2", e2).contains("OAUTH_CORREO_NO_VERIFICADO"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void callbackConCodigoVacioRedirigeConError() {
        String estado = servicio.urlGoogle().state();
        assertTrue(servicio.callbackGoogle("", estado).contains("OAUTH_CODIGO_INVALIDO"));
    }

    @Test
    void callbackReactivaBloqueoVencido() {
        Usuario usuario = usuarioGoogle();
        usuario.setEstado("BLOQUEADO");
        usuario.setBloqueadoHasta(Instant.now().minusSeconds(60));
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        String ticket = ticketDe("sub-1", "ana@correo.com");
        assertTrue(ticket.length() > 10);
        assertEquals("ACTIVO", usuario.getEstado());
    }

    @Test
    void callbackCreaConNombreNuloUsaCorreoYGuion() {
        stubCrear("ana@correo.com");
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(
                new VerificadorGoogle.CuentaGoogle("sub-x", "ana@correo.com", true, null, null));
        String destino = servicio.callbackGoogle("code", estado);
        assertTrue(destino.contains("?ticket="), destino);
        ArgumentCaptor<Perfil> perfil = ArgumentCaptor.forClass(Perfil.class);
        verify(perfiles).save(perfil.capture());
        assertEquals("ana", perfil.getValue().getNombres());
        assertEquals("-", perfil.getValue().getApellidos());
    }

    @Test
    void configuradoExigeLosTresValores() {
        assertTrue(oauth.configurado());
        oauth.setClientSecret("");
        assertFalse(oauth.configurado());
        oauth.setClientSecret("x");
        oauth.setRedirectUri("  ");
        assertFalse(oauth.configurado());
        oauth.setRedirectUri("http://x/callback");
        oauth.setClientId(null);
        assertFalse(oauth.configurado());
        oauth.setClientId(CLIENTE_ID);
        oauth.setRedirectUri(null);
        assertFalse(oauth.configurado());
        oauth.setRedirectUri("http://x/callback");
        oauth.setClientId("${GOOGLE_OAUTH_CLIENT_ID}");
        assertFalse(oauth.configurado(), "un placeholder sin resolver no es configuración");
    }
    @Test
    void callbackConCuentaBloqueadaRedirigeConError() {
        Usuario usuario = usuarioGoogle();
        usuario.setEstado("BLOQUEADO");
        usuario.setBloqueadoHasta(Instant.now().plusSeconds(3600));
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta("sub-1", "ana@correo.com"));
        assertTrue(servicio.callbackGoogle("code", estado).contains("CUENTA_BLOQUEADA"));
    }

    @Test
    void callbackConCuentaBloqueadaPorAdministradorRedirigeConError() {
        Usuario usuario = usuarioGoogle();
        usuario.setEstado("BLOQUEADO");
        usuario.setBloqueadoHasta(null);
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta("sub-1", "ana@correo.com"));
        assertTrue(servicio.callbackGoogle("code", estado).contains("CUENTA_BLOQUEADA"));
        assertEquals("BLOQUEADO", usuario.getEstado());
    }

    @Test
    void callbackConCuentaInactivaRedirigeConError() {
        Usuario usuario = usuarioGoogle();
        usuario.setEstado("INACTIVO");
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta("sub-1", "ana@correo.com"));
        assertTrue(servicio.callbackGoogle("code", estado).contains("CUENTA_INACTIVA"));
    }

    // ---------- callback: vincular o crear ----------

    @Test
    void callbackConSubConocidoEmiteTicketCanjeable() {
        Usuario usuario = usuarioGoogle();
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt-test");
        String ticket = ticketDe("sub-1", "ana@correo.com");
        SesionResponse sesion = (SesionResponse) servicio.canjearTicketGoogle(new CanjeTicketRequest(ticket));
        assertEquals("jwt-test", sesion.tokenAcceso());
        assertNotNull(usuario.getUltimoAcceso());
    }

    @Test
    void callbackVinculaCorreoLocalSinElevarRoles() {
        Usuario local = usuarioLocal();
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-9")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(local));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(local));
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwt.generar(any())).thenReturn("jwt-test");
        String ticket = ticketDe("sub-9", "ana@correo.com");
        servicio.canjearTicketGoogle(new CanjeTicketRequest(ticket));
        assertEquals("GOOGLE", local.getProveedor());
        assertEquals("sub-9", local.getProveedorId());
        assertEquals(1, local.getRoles().size());
    }

    @Test
    void callbackVinculaCuentaSinFotoYUsaLaDeGoogle() {
        Usuario local = usuarioLocal();
        Perfil perfil = perfilCon(null);
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-9")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(local));
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfiles.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        ticketDe("sub-9", "ana@correo.com");
        assertEquals("http://foto/x.jpg", perfil.getFotoUrl());
        verify(perfiles).save(perfil);
    }

    @Test
    void callbackNoReemplazaLaFotoPropia() {
        Usuario usuario = usuarioGoogle();
        Perfil perfil = perfilCon("https://stproyecto2fotos.blob.core.windows.net/fotos/perfiles/propia.jpg");
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        when(perfiles.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        ticketDe("sub-1", "ana@correo.com");
        assertEquals("https://stproyecto2fotos.blob.core.windows.net/fotos/perfiles/propia.jpg", perfil.getFotoUrl());
        verify(perfiles, never()).save(any(Perfil.class));
    }

    @Test
    void callbackConFotoVaciaDeGoogleNoTocaElPerfil() {
        Usuario usuario = usuarioGoogle();
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        for (String foto : new String[] {null, " "}) {
            String estado = servicio.urlGoogle().state();
            when(google.intercambiarCodigo("code")).thenReturn("idtoken");
            when(google.verificar("idtoken")).thenReturn(
                    new VerificadorGoogle.CuentaGoogle("sub-1", "ana@correo.com", true, "Ana", foto));
            assertTrue(servicio.callbackGoogle("code", estado).contains("?ticket="));
        }
        verify(perfiles, never()).findById(any());
    }

    @Test
    void callbackRechazaCorreoVinculadoAOtroSub() {
        Usuario vinculado = usuarioGoogle();
        vinculado.setProveedorId("sub-otro");
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-9")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(vinculado));
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta("sub-9", "ana@correo.com"));
        assertTrue(servicio.callbackGoogle("code", estado).contains("OAUTH_CUENTA_VINCULADA"));
    }

    @Test
    void callbackCreaClienteConCarteraYPublicaCuenta() {
        stubCrear("nueva@correo.com");
        String ticket = ticketDe("sub-nuevo", "nueva@correo.com");
        assertTrue(ticket.length() > 10);
        verify(carteras).save(any(Cartera.class));
        verify(publicador).publicarCuentaCreada(any(), any(), any(), any(), any(), any());
        verify(publicador, never()).publicarClienteRegistrado(any(), any(), any(), any(), any());
        ArgumentCaptor<Perfil> perfil = ArgumentCaptor.forClass(Perfil.class);
        verify(perfiles).save(perfil.capture());
        assertEquals("Ana", perfil.getValue().getNombres());
        assertEquals("Pérez Gómez", perfil.getValue().getApellidos());
    }

    @Test
    void callbackConDobleFactorDevuelveDesafioAlCanjear() {
        Usuario usuario = usuarioGoogle();
        usuario.setDobleFactorActivo(true);
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        String ticket = ticketDe("sub-1", "ana@correo.com");
        ResultadoInicioSesion resultado = servicio.canjearTicketGoogle(new CanjeTicketRequest(ticket));
        assertInstanceOf(DesafioDobleFactor.class, resultado);
        verify(jwt, never()).generar(any());
    }

    // ---------- canjear: errores ----------

    @Test
    void canjearTicketInvalidoDa401() {
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.canjearTicketGoogle(new CanjeTicketRequest("inexistente")));
        assertEquals("OAUTH_TICKET_INVALIDO", error.getCodigo());
    }

    @Test
    void canjearTicketVencidoDa401() {
        oauth.setTicketTtlSegundos(-1);
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1"))
                .thenReturn(Optional.of(usuarioGoogle()));
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta("sub-1", "ana@correo.com"));
        String destino = servicio.callbackGoogle("code", estado);
        String ticket = destino.substring(destino.indexOf("ticket=") + 7);
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.canjearTicketGoogle(new CanjeTicketRequest(ticket)));
        assertEquals("OAUTH_TICKET_INVALIDO", error.getCodigo());
    }

    @Test
    void canjearTicketReusadoDa401() {
        Usuario usuario = usuarioGoogle();
        when(usuarios.findByProveedorAndProveedorId("GOOGLE", "sub-1")).thenReturn(Optional.of(usuario));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn("jwt-test");
        String ticket = ticketDe("sub-1", "ana@correo.com");
        servicio.canjearTicketGoogle(new CanjeTicketRequest(ticket));
        assertThrows(NoAutenticadoException.class,
                () -> servicio.canjearTicketGoogle(new CanjeTicketRequest(ticket)));
    }

    // ---------- coherencia con claves nulas ----------

    @Test
    void loginConClaveEnCuentaOAuthDaCredencialesInvalidas() {
        Usuario usuario = usuarioGoogle();
        usuario.setClaveCifrada(null);
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        NoAutenticadoException error = assertThrows(NoAutenticadoException.class,
                () -> servicio.iniciarSesion(new InicioSesionRequest("ana@correo.com", "x")));
        assertEquals("CREDENCIALES_INVALIDAS", error.getCodigo());
    }

    // ---------- VerificadorGoogle con JWT reales ----------

    @Test
    void verificadorIntercambiaYValidaFirmaAudEmisorYVigencia() throws Exception {
        RSAKey llave = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        String jwks = "{\"keys\":[" + llave.toPublicJWK().toJSONString() + "]}";
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS)).thenReturn(jwks);
        String token = firmar(llave, CLIENTE_ID, "accounts.google.com",
                Date.from(Instant.now().plusSeconds(300)), null);
        when(httpGoogle.postFormulario(any(), any())).thenReturn(Map.of("id_token", token));

        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        assertEquals(token, verificador.intercambiarCodigo("code-ok"));
        VerificadorGoogle.CuentaGoogle cuenta = verificador.verificar(token);
        assertEquals("sub-1", cuenta.sub());
        assertEquals("ana@correo.com", cuenta.correo());
        assertTrue(cuenta.verificado());
    }

    @Test
    void verificadorRechazaFirmaDeOtraLlave() throws Exception {
        RSAKey publicada = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        RSAKey otra = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS))
                .thenReturn("{\"keys\":[" + publicada.toPublicJWK().toJSONString() + "]}");
        String token = firmar(otra, CLIENTE_ID, "accounts.google.com",
                Date.from(Instant.now().plusSeconds(300)), null);
        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(token)).getCodigo());
    }

    @Test
    void verificadorRechazaAudienciaAjenaYTokenVencido() throws Exception {
        RSAKey llave = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS))
                .thenReturn("{\"keys\":[" + llave.toPublicJWK().toJSONString() + "]}");
        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        String ajeno = firmar(llave, "otro-cliente", "accounts.google.com",
                Date.from(Instant.now().plusSeconds(300)), null);
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(ajeno)).getCodigo());
        String vencido = firmar(llave, CLIENTE_ID, "https://accounts.google.com",
                Date.from(Instant.now().minusSeconds(600)), null);
        assertEquals("GOOGLE_TOKEN_VENCIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(vencido)).getCodigo());
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar("no-es-jwt")).getCodigo());
    }

    @Test
    void verificadorRechazaEmisorAjenaSinKidYAlgNone() throws Exception {
        RSAKey llave = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS))
                .thenReturn("{\"keys\":[" + llave.toPublicJWK().toJSONString() + "]}");
        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        String emisorAjeno = firmar(llave, CLIENTE_ID, "https://evil.example.com",
                Date.from(Instant.now().plusSeconds(300)), null);
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(emisorAjeno)).getCodigo());
        String sinKid = tokenManual("{\"alg\":\"RS256\"}", cuerpoValido());
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(sinKid)).getCodigo());
        String algNone = tokenManual("{\"alg\":\"none\"}", cuerpoValido());
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(algNone)).getCodigo());
    }

    @Test
    void verificadorRechazaSinExpiracionEmisionFuturaYClaveNoRsa() throws Exception {
        RSAKey llave = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS))
                .thenReturn("{\"keys\":[" + llave.toPublicJWK().toJSONString() + "]}");
        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        String sinExp = firmar(llave, CLIENTE_ID, "accounts.google.com", null, null);
        assertEquals("GOOGLE_TOKEN_VENCIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(sinExp)).getCodigo());
        String futuro = firmar(llave, CLIENTE_ID, "accounts.google.com",
                Date.from(Instant.now().plusSeconds(900)), Date.from(Instant.now().plusSeconds(600)));
        assertEquals("GOOGLE_TOKEN_VENCIDO", assertThrows(NoAutenticadoException.class,
                () -> verificador.verificar(futuro)).getCodigo());
        String oct = firmarCrudo(llave,
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid-otro").build(), cuerpoValido());
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS)).thenReturn(
                "{\"keys\":[{\"kty\":\"oct\",\"k\":\"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY\",\"kid\":\"kid-otro\"}]}");
        VerificadorGoogle otro = new VerificadorGoogle(httpGoogle, oauth);
        assertEquals("GOOGLE_TOKEN_INVALIDO", assertThrows(NoAutenticadoException.class,
                () -> otro.verificar(oct)).getCodigo());
    }

    @Test
    void verificadorAceptaEmailVerificadoComoTexto() throws Exception {
        RSAKey llave = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS))
                .thenReturn("{\"keys\":[" + llave.toPublicJWK().toJSONString() + "]}");
        String cuerpo = cuerpoValido().replace("\"email_verified\":true", "\"email_verified\":\"true\"");
        String token = firmarCrudo(llave,
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid-1").build(), cuerpo);
        VerificadorGoogle.CuentaGoogle cuenta = new VerificadorGoogle(httpGoogle, oauth).verificar(token);
        assertTrue(cuenta.verificado());
    }
    @Test
    void verificadorSinRespuestaDeGoogleDa503() {
        when(httpGoogle.postFormulario(any(), any())).thenThrow(new RuntimeException("caído"));
        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        assertEquals("GOOGLE_SIN_RESPUESTA", assertThrows(ServicioNoDisponibleException.class,
                () -> verificador.intercambiarCodigo("code")).getCodigo());
        // doReturn: el stub anterior con thenThrow se dispararía al re-stubear con when().
        doReturn(Map.of("nada", "aquí")).when(httpGoogle).postFormulario(any(), any());
        assertEquals("GOOGLE_SIN_ID_TOKEN", assertThrows(NoAutenticadoException.class,
                () -> verificador.intercambiarCodigo("code")).getCodigo());
        doReturn(Map.of("id_token", "  ")).when(httpGoogle).postFormulario(any(), any());
        assertEquals("GOOGLE_SIN_ID_TOKEN", assertThrows(NoAutenticadoException.class,
                () -> verificador.intercambiarCodigo("code")).getCodigo());
    }

    @Test
    void verificadorSinLlavesDeGoogleDa503() throws Exception {
        RSAKey llave = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(httpGoogle.getTexto(VerificadorGoogle.URL_JWKS)).thenThrow(new RuntimeException("caído"));
        VerificadorGoogle verificador = new VerificadorGoogle(httpGoogle, oauth);
        String token = firmar(llave, CLIENTE_ID, "accounts.google.com",
                Date.from(Instant.now().plusSeconds(300)), null);
        assertEquals("GOOGLE_SIN_RESPUESTA", assertThrows(ServicioNoDisponibleException.class,
                () -> verificador.verificar(token)).getCodigo());
    }

    // ---------- apoyos ----------

    private VerificadorGoogle.CuentaGoogle cuenta(String sub, String correo) {
        return new VerificadorGoogle.CuentaGoogle(sub, correo, true, "Ana Pérez Gómez", "http://foto/x.jpg");
    }

    private String firmar(RSAKey llave, String audiencia, String emisor, Date expira, Date emitido)
            throws Exception {
        JWTClaimsSet.Builder constructor = new JWTClaimsSet.Builder()
                .subject("sub-1")
                .issuer(emisor)
                .audience(audiencia)
                .claim("email", "ana@correo.com")
                .claim("email_verified", true)
                .claim("name", "Ana Pérez");
        if (expira != null) {
            constructor.expirationTime(expira);
        }
        if (emitido != null) {
            constructor.issueTime(emitido);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid-1").build(),
                constructor.build());
        jwt.sign(new RSASSASigner(llave));
        return jwt.serialize();
    }

    private String firmarCrudo(RSAKey llave, JWSHeader cabecera, String cuerpoJson) throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse(cuerpoJson);
        SignedJWT jwt = new SignedJWT(cabecera, claims);
        jwt.sign(new RSASSASigner(llave));
        return jwt.serialize();
    }

    /** Token armado a mano (firma basura válida en base64) para casos que Nimbus no deja firmar. */
    private String tokenManual(String cabeceraJson, String cuerpoJson) {
        java.util.Base64.Encoder b64 = java.util.Base64.getUrlEncoder().withoutPadding();
        java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
        return b64.encodeToString(cabeceraJson.getBytes(utf8)) + "."
                + b64.encodeToString(cuerpoJson.getBytes(utf8)) + ".ZmlybWE";
    }

    private String cuerpoValido() {
        long exp = Instant.now().plusSeconds(300).getEpochSecond();
        return "{\"sub\":\"sub-1\",\"iss\":\"accounts.google.com\",\"aud\":\"" + CLIENTE_ID + "\","
                + "\"exp\":" + exp + ",\"email\":\"ana@correo.com\","
                + "\"email_verified\":true,\"name\":\"Ana\"}";
    }

    private void stubCrear(String correo) {
        when(usuarios.findByProveedorAndProveedorId(any(), any())).thenReturn(Optional.empty());
        when(usuarios.findByCorreo(correo)).thenReturn(Optional.empty());
        Rol cliente = new Rol();
        cliente.setCodigo(Roles.CLIENTE);
        when(roles.findById(Roles.CLIENTE)).thenReturn(Optional.of(cliente));
    }

    private String ticketDe(String sub, String correo) {
        String estado = servicio.urlGoogle().state();
        when(google.intercambiarCodigo("code")).thenReturn("idtoken");
        when(google.verificar("idtoken")).thenReturn(cuenta(sub, correo));
        String destino = servicio.callbackGoogle("code", estado);
        assertTrue(destino.contains("?ticket="), destino);
        return destino.substring(destino.indexOf("ticket=") + 7);
    }

    private Usuario usuarioGoogle() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setCorreo("ana@correo.com");
        usuario.setClaveCifrada(null);
        usuario.setEstado("ACTIVO");
        usuario.setProveedor("GOOGLE");
        usuario.setProveedorId("sub-1");
        Rol cliente = new Rol();
        cliente.setCodigo(Roles.CLIENTE);
        usuario.getRoles().add(cliente);
        return usuario;
    }

    private Perfil perfilCon(String fotoUrl) {
        Perfil perfil = new Perfil();
        perfil.setNombres("Ana");
        perfil.setApellidos("Pérez");
        perfil.setFotoUrl(fotoUrl);
        return perfil;
    }

    private Usuario usuarioLocal() {
        Usuario usuario = usuarioGoogle();
        usuario.setClaveCifrada(claves.encode("Segura#2026"));
        usuario.setProveedor("LOCAL");
        usuario.setProveedorId(null);
        return usuario;
    }
}
