package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.auth.*;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioSesion;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.CodigoDobleFactor;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.TokenRefresco;
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
import gt.cunoc.proyecto2.identity.services.oauth.GoogleOAuthProperties;
import gt.cunoc.proyecto2.identity.services.oauth.VerificadorGoogle;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sesión con bloqueo por intentos, registro público y tokens de sesión.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Duration REFRESCO_DURACION = Duration.ofDays(7);
    private static final Duration RECUPERACION_DURACION = Duration.ofMinutes(30);
    private static final Duration DOBLE_FACTOR_DURACION = Duration.ofMinutes(10);
    private static final SecureRandom SEGURIDAD = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfiles;
    private final RolRepository roles;
    private final CarteraRepository carteras;
    private final TokenRefrescoRepository refrescos;
    private final TokenRecuperacionRepository recuperaciones;
    private final CodigoDobleFactorRepository desafios;
    private final PasswordEncoder claves;
    private final ServicioJwt jwt;
    private final long minutosAcceso;
    private final IdentidadUsuarioFactory fabrica;
    private final TokenOpacoService opacos;
    private final VigilanciaAcceso vigilancia;
    private final UsuarioEventPublisher publicador;
    private final UserService servicioUsuarios;
    private final String urlFrontend;
    private final GoogleOAuthProperties oauth;
    private final CodigosUnSoloUso codigos;
    private final VerificadorGoogle google;

    public AuthServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            CarteraRepository carteras,
            TokenRefrescoRepository refrescos,
            TokenRecuperacionRepository recuperaciones,
            CodigoDobleFactorRepository desafios,
            PasswordEncoder claves,
            ServicioJwt jwt,
            @Value("${app.jwt.duracion-minutos:60}") long minutosAcceso,
            IdentidadUsuarioFactory fabrica,
            TokenOpacoService opacos,
            VigilanciaAcceso vigilancia,
            UsuarioEventPublisher publicador,
            UserService servicioUsuarios,
            @Value("${FRONTEND_URL:http://localhost:5173}") String urlFrontend,
            GoogleOAuthProperties oauth,
            CodigosUnSoloUso codigos,
            VerificadorGoogle google) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
        this.refrescos = refrescos;
        this.recuperaciones = recuperaciones;
        this.desafios = desafios;
        this.claves = claves;
        this.jwt = jwt;
        this.minutosAcceso = minutosAcceso;
        this.fabrica = fabrica;
        this.opacos = opacos;
        this.vigilancia = vigilancia;
        this.publicador = publicador;
        this.servicioUsuarios = servicioUsuarios;
        this.urlFrontend = urlFrontend;
        this.oauth = oauth;
        this.codigos = codigos;
        this.google = google;
    }

    @Override
    @Transactional
    public RegistroResponse registrar(RegistroRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();

        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException("CORREO_EN_USO", "El correo ya está registrado");
        }
        if (perfiles.existsByNit(solicitud.nit())) {
            throw new ConflictoException("NIT_EN_USO", "El NIT ya está registrado");
        }
        if (perfiles.existsByDpi(solicitud.dpi())) {
            throw new ConflictoException("DPI_EN_USO", "El DPI ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setClaveCifrada(claves.encode(solicitud.contrasena()));
        usuario.setEstado("ACTIVO");
        Rol cliente = roles.findById(Roles.CLIENTE)
                .orElseThrow(() -> new ReglaNegocioException("ROL_FALTANTE", "No existe el rol CLIENTE"));
        usuario.getRoles().add(cliente);
        usuarios.save(usuario);

        Perfil perfil = solicitud.createEntity(usuario);
        perfil.setFotoUrl(solicitud.fotoUrl());
        perfiles.save(perfil);

        Cartera cartera = new Cartera();
        cartera.setUsuarioId(usuario.getId());
        cartera.setSaldo(BigDecimal.ZERO);
        carteras.save(cartera);

        String nombre = solicitud.nombres() + " " + solicitud.apellidos();
        publicador.publicarCuentaCreada(
                usuario.getId(), correo, nombre, List.of(Roles.CLIENTE), null,
                LocalDate.now());
        publicador.publicarClienteRegistrado(
                usuario.getId(), solicitud.nit(), nombre, correo,
                LocalDate.now());
        return new RegistroResponse(usuario.getId(), correo, Set.of(Roles.CLIENTE));
    }

    @Override
    @Transactional
    public ResultadoInicioSesion iniciarSesion(InicioSesionRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        Usuario usuario = usuarios.findByCorreo(correo).orElse(null);

        if (usuario == null || usuario.getClaveCifrada() == null
                || !claves.matches(solicitud.contrasena(), usuario.getClaveCifrada())) {
            if (usuario != null) {
                vigilancia.falloContrasena(usuario.getId());
            }
            throw new NoAutenticadoException("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
        }
        exigirCuentaUtilizable(usuario);

        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(Instant.now());
        usuarios.save(usuario);

        if (usuario.isDobleFactorActivo()) {
            return desafiarDobleFactor(usuario);
        }
        return crearTokens(usuario);
    }

    @Override
    @Transactional
    public SesionResponse verificarDobleFactor(VerificarDobleFactorRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        Usuario usuario = usuarios.findByCorreo(correo).orElse(null);

        if (usuario == null || !"ACTIVO".equals(usuario.getEstado()) || !usuario.isDobleFactorActivo()) {
            throw new NoAutenticadoException("DOBLE_FACTOR_INVALIDO", "El código no es válido");
        }

        CodigoDobleFactor desafio = desafios
                .findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(usuario.getId())
                .stream().findFirst()
                .orElseThrow(() -> new NoAutenticadoException("DOBLE_FACTOR_INVALIDO", "El código no es válido"));

        if (desafio.getExpiraEn().isBefore(Instant.now())) {
            throw new NoAutenticadoException("DOBLE_FACTOR_VENCIDO", "El código venció, inicie sesión de nuevo");
        }

        if (!opacos.calcularHash(solicitud.codigo()).equals(desafio.getCodigoHash())) {
            vigilancia.falloCodigo(desafio.getId(), usuario.getId());
            throw new NoAutenticadoException("DOBLE_FACTOR_INVALIDO", "El código no es válido");
        }

        desafio.setUsadoEn(Instant.now());
        desafios.save(desafio);
        usuario.setUltimoAcceso(Instant.now());
        usuarios.save(usuario);
        return crearTokens(usuario);
    }

    @Override
    @Transactional
    public void cambiarDobleFactor(UUID usuarioId, DobleFactorRequest solicitud) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        usuario.setDobleFactorActivo(Boolean.TRUE.equals(solicitud.activado()));
        usuarios.save(usuario);
    }

    @Override
    @Transactional
    public SesionResponse renovarSesion(TokenRefrescoRequest solicitud) {
        TokenRefresco guardado = refrescos.findByTokenHash(opacos.calcularHash(solicitud.tokenRefresco()))
                .orElseThrow(() -> new NoAutenticadoException("TOKEN_REFRESCO_INVALIDO", "La sesión no es válida"));

        if (guardado.getRevocadoEn() != null || guardado.getExpiraEn().isBefore(Instant.now())) {
            throw new NoAutenticadoException("TOKEN_REFRESCO_INVALIDO", "La sesión no es válida");
        }

        Usuario usuario = usuarios.findById(guardado.getUsuarioId())
                .orElseThrow(() -> new NoAutenticadoException("TOKEN_REFRESCO_INVALIDO", "La sesión no es válida"));

        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new NoAutenticadoException("CUENTA_INACTIVA", "La cuenta no está activa");
        }

        guardado.setRevocadoEn(Instant.now());
        refrescos.save(guardado);
        return crearTokens(usuario);
    }

    @Override
    @Transactional
    public void cerrarSesion(UUID usuarioId, TokenRefrescoRequest solicitud) {
        TokenRefresco guardado = refrescos.findByTokenHash(opacos.calcularHash(solicitud.tokenRefresco())).orElse(null);

        if (guardado != null && guardado.getUsuarioId().equals(usuarioId) && guardado.getRevocadoEn() == null) {
            guardado.setRevocadoEn(Instant.now());
            refrescos.save(guardado);
        }
    }

    @Override
    @Transactional
    public void solicitarRecuperacion(RecuperacionRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        Usuario usuario = usuarios.findByCorreo(correo).orElse(null);

        if (usuario == null || !"ACTIVO".equals(usuario.getEstado())) {
            return;
        }

        String crudo = opacos.generar();
        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuarioId(usuario.getId());
        token.setTokenHash(opacos.calcularHash(crudo));
        token.setExpiraEn(Instant.now().plus(RECUPERACION_DURACION));
        recuperaciones.save(token);
        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        String nombre = perfil == null ? correo : perfil.getNombres() + " " + perfil.getApellidos();
        publicador.publicarRecuperacionContrasenaSolicitada(
                usuario.getId(), correo, nombre,
                urlFrontend + "/restablecer?token=" + crudo, token.getExpiraEn(),
                LocalDate.now());
    }

    @Override
    @Transactional
    public void restablecerContrasena(RestablecimientoRequest solicitud) {
        TokenRecuperacion token = recuperaciones.findByTokenHash(opacos.calcularHash(solicitud.token()))
                .orElseThrow(() -> new NoEncontradoException("TOKEN_NO_EXISTE", "El enlace no existe"));
        if (token.getExpiraEn().isBefore(Instant.now())) {
            throw new ReglaNegocioException("TOKEN_VENCIDO", "El enlace expiró, solicite otro");
        }
        if (token.getUsadoEn() != null) {
            throw new ReglaNegocioException("TOKEN_USADO", "El enlace ya fue utilizado");
        }
        Usuario usuario = usuarios.findById(token.getUsuarioId())
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        usuario.setClaveCifrada(claves.encode(solicitud.nuevaContrasena()));
        usuario.setIntentosFallidos(0);
        // Cambiar la contraseña quita el bloqueo por intentos fallidos, no el que puso un administrador.
        if ("BLOQUEADO".equals(usuario.getEstado()) && usuario.getBloqueadoHasta() != null) {
            usuario.setEstado("ACTIVO");
            usuario.setBloqueadoHasta(null);
        }
        usuarios.save(usuario);
        token.setUsadoEn(Instant.now());
        recuperaciones.save(token);
        revocarRefrescos(usuario.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse miPerfil(UUID usuarioId) {
        return servicioUsuarios.perfil(usuarioId);
    }

    @Override
    @Transactional
    public void cambiarContrasena(UUID usuarioId, CambioContrasenaRequest solicitud) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));

        if (usuario.getClaveCifrada() == null
                || !claves.matches(solicitud.contrasenaActual(), usuario.getClaveCifrada())) {
            throw new ReglaNegocioException("CONTRASENA_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
        }

        usuario.setClaveCifrada(claves.encode(solicitud.nuevaContrasena()));
        usuarios.save(usuario);
    }

    @Override
    public UrlGoogleResponse urlGoogle() {
        exigirOAuth();
        String estado = codigos.emitirEstado(oauth.getStateTtlSegundos());
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + codificar(oauth.getClientId())
                + "&redirect_uri=" + codificar(oauth.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + codificar("openid email profile")
                + "&state=" + codificar(estado)
                + "&access_type=online&prompt=select_account";
        return new UrlGoogleResponse(url, estado);
    }

    @Override
    @Transactional
    public String callbackGoogle(String codigo, String estado) {
        // Todo error vuelve al frontend como ?error=CODIGO (nunca expone detalle interno).
        if (!oauth.configurado()) {
            return destinoOAuth("error", "OAUTH_NO_CONFIGURADO");
        }
        try {
            if (codigos.consumirEstado(estado) == null) {
                return destinoOAuth("error", "OAUTH_ESTADO_INVALIDO");
            }
            if (codigo == null || codigo.isBlank()) {
                return destinoOAuth("error", "OAUTH_CODIGO_INVALIDO");
            }
            VerificadorGoogle.CuentaGoogle cuenta = google.verificar(google.intercambiarCodigo(codigo));
            if (cuenta.correo() == null || cuenta.correo().isBlank() || !cuenta.verificado()) {
                return destinoOAuth("error", "OAUTH_CORREO_NO_VERIFICADO");
            }
            Usuario usuario = vincularOCrear(cuenta);
            completarFotoGoogle(usuario, cuenta.foto());
            exigirActivaOAuth(usuario);
            usuario.setUltimoAcceso(Instant.now());
            usuarios.save(usuario);
            String ticket = codigos.emitirTicket(
                    new CodigosUnSoloUso.Ticket(usuario.getId(), usuario.isDobleFactorActivo()),
                    oauth.getTicketTtlSegundos());
            return destinoOAuth("ticket", ticket);
        } catch (NegocioException e) {
            return destinoOAuth("error", e.getCodigo());
        }
    }

    @Override
    @Transactional
    public ResultadoInicioSesion canjearTicketGoogle(CanjeTicketRequest solicitud) {
        CodigosUnSoloUso.Ticket ticket = codigos.consumirTicket(solicitud.ticket());
        if (ticket == null) {
            throw new NoAutenticadoException("OAUTH_TICKET_INVALIDO",
                    "El ticket venció o ya fue usado");
        }
        Usuario usuario = usuarios.findById(ticket.usuarioId())
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        exigirActivaOAuth(usuario);
        usuario.setUltimoAcceso(Instant.now());
        usuarios.save(usuario);
        if (ticket.dobleFactorPendiente() && usuario.isDobleFactorActivo()) {
            return desafiarDobleFactor(usuario);
        }
        return crearTokens(usuario);
    }

    private void exigirOAuth() {
        if (!oauth.configurado()) {
            throw new ServicioNoDisponibleException("OAUTH_NO_CONFIGURADO",
                    "El inicio con Google no está configurado");
        }
    }

    private String destinoOAuth(String parametro, String valor) {
        return oauth.getFrontendUrl() + "/oauth/google/callback?" + parametro + "="
                + codificar(valor);
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    /**
     * Si la cuenta no tiene foto propia (p. ej. una cuenta LOCAL que se acaba de vincular),
     * usa la foto de Google. Una foto ya guardada (subida al bucket) nunca se reemplaza.
     */
    private void completarFotoGoogle(Usuario usuario, String foto) {
        if (foto == null || foto.isBlank()) {
            return;
        }
        perfiles.findById(usuario.getId())
                .filter(perfil -> perfil.getFotoUrl() == null || perfil.getFotoUrl().isBlank())
                .ifPresent(perfil -> {
                    perfil.setFotoUrl(foto);
                    perfiles.save(perfil);
                });
    }

    /** Igual que iniciarSesion (mismas reglas de bloqueo). */
    private void exigirActivaOAuth(Usuario usuario) {
        exigirCuentaUtilizable(usuario);
    }

    /**
     * Reglas de entrada según el estado de la cuenta:
     * <ul>
     *   <li>BLOQUEADO sin fecha: lo bloqueó un administrador; no vence, solo él lo quita (401).</li>
     *   <li>BLOQUEADO con fecha futura: bloqueo por intentos fallidos todavía vigente (401).</li>
     *   <li>BLOQUEADO con fecha pasada: el bloqueo por intentos venció; la cuenta se reactiva.</li>
     *   <li>Cualquier otro estado distinto de ACTIVO: 401 CUENTA_INACTIVA.</li>
     * </ul>
     */
    private void exigirCuentaUtilizable(Usuario usuario) {
        if ("BLOQUEADO".equals(usuario.getEstado())) {
            if (usuario.getBloqueadoHasta() == null) {
                throw new NoAutenticadoException("CUENTA_BLOQUEADA",
                        "La cuenta fue bloqueada por un administrador");
            }
            if (usuario.getBloqueadoHasta().isAfter(Instant.now())) {
                throw new NoAutenticadoException("CUENTA_BLOQUEADA",
                        "La cuenta está bloqueada hasta " + usuario.getBloqueadoHasta());
            }
            usuario.setEstado("ACTIVO");
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
        }
        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new NoAutenticadoException("CUENTA_INACTIVA", "La cuenta no está activa");
        }
    }

    /**
     * Regla de enlace: mismo sub de Google -&gt; entra; correo LOCAL sin vínculo -&gt; se vincula
     * (conserva sus roles, nunca eleva); correo ya vinculado a otro sub -&gt; conflicto;
     * correo nuevo -&gt; se crea como CLIENTE.
     */
    private Usuario vincularOCrear(VerificadorGoogle.CuentaGoogle cuenta) {
        String correo = cuenta.correo().trim().toLowerCase();
        Optional<Usuario> porSub = usuarios.findByProveedorAndProveedorId("GOOGLE", cuenta.sub());
        if (porSub.isPresent()) {
            return porSub.get();
        }
        Usuario existente = usuarios.findByCorreo(correo).orElse(null);
        if (existente != null) {
            if (!"LOCAL".equals(existente.getProveedor()) || existente.getProveedorId() != null) {
                throw new ConflictoException("OAUTH_CUENTA_VINCULADA",
                        "El correo ya está vinculado a otra cuenta externa");
            }
            existente.setProveedor("GOOGLE");
            existente.setProveedorId(cuenta.sub());
            return usuarios.save(existente);
        }
        return crearDesdeGoogle(cuenta, correo);
    }

    private Usuario crearDesdeGoogle(VerificadorGoogle.CuentaGoogle cuenta, String correo) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setClaveCifrada(null);
        usuario.setEstado("ACTIVO");
        usuario.setProveedor("GOOGLE");
        usuario.setProveedorId(cuenta.sub());
        Rol cliente = roles.findById(Roles.CLIENTE)
                .orElseThrow(() -> new ReglaNegocioException("ROL_FALTANTE", "No existe el rol CLIENTE"));
        usuario.getRoles().add(cliente);
        usuarios.save(usuario);

        String nombre = cuenta.nombre() == null ? "" : cuenta.nombre().trim();
        if (nombre.isBlank()) {
            nombre = correo.substring(0, correo.indexOf('@'));
        }
        String[] partes = nombre.split("\\s+", 2);
        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setNombres(partes[0]);
        perfil.setApellidos(partes.length > 1 ? partes[1] : "-");
        perfil.setFotoUrl(cuenta.foto());
        perfiles.save(perfil);

        Cartera cartera = new Cartera();
        cartera.setUsuarioId(usuario.getId());
        cartera.setSaldo(BigDecimal.ZERO);
        carteras.save(cartera);

        publicador.publicarCuentaCreada(usuario.getId(), correo,
                (perfil.getNombres() + " " + perfil.getApellidos()).trim(),
                List.of(Roles.CLIENTE), null, LocalDate.now());
        // Sin NIT no se publica ClienteRegistrado (sales exige nit): al completar el
        // perfil, PerfilActualizado lo da de alta en ventas con el upsert existente.
        return usuario;
    }

    private DesafioDobleFactor desafiarDobleFactor(Usuario usuario) {        String codigo = String.format("%06d", SEGURIDAD.nextInt(1_000_000));
        CodigoDobleFactor desafio = new CodigoDobleFactor();
        desafio.setUsuarioId(usuario.getId());
        desafio.setCodigoHash(opacos.calcularHash(codigo));
        desafio.setExpiraEn(Instant.now().plus(DOBLE_FACTOR_DURACION));
        desafio.setFechaCreacion(Instant.now());
        desafios.save(desafio);

        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        String nombre = perfil == null ? usuario.getCorreo()
                : perfil.getNombres() + " " + perfil.getApellidos();
        publicador.publicarCodigoDobleFactor(
                usuario.getId(), usuario.getCorreo(), nombre, codigo,
                desafio.getExpiraEn(), LocalDate.now());
        return new DesafioDobleFactor(usuario.getId(), usuario.getCorreo(), desafio.getExpiraEn());
    }

    private void revocarRefrescos(UUID usuarioId) {
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuarioId).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }

    private SesionResponse crearTokens(Usuario usuario) {
        UsuarioActual actual = fabrica.construir(usuario);
        String acceso = jwt.generar(actual);
        String crudo = opacos.generar();
        TokenRefresco refresco = new TokenRefresco();
        refresco.setUsuarioId(usuario.getId());
        refresco.setTokenHash(opacos.calcularHash(crudo));
        refresco.setExpiraEn(Instant.now().plus(REFRESCO_DURACION));
        refrescos.save(refresco);

        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        Set<String> roles = actual.roles().stream().sorted().collect(Collectors.toSet());
        String nombre = perfil == null ? usuario.getCorreo() : perfil.getNombres() + " " + perfil.getApellidos();
        UsuarioSesion sesion = new UsuarioSesion(
                usuario.getId(), usuario.getCorreo(), nombre, roles,
                actual.sucursalId(), actual.choferId(),
                perfil == null ? null : perfil.getFotoUrl());
        Instant expira = Instant.now().plus(Duration.ofMinutes(minutosAcceso));
        String expiraEn = DateTimeFormatter.ISO_INSTANT.format(expira.atOffset(ZoneOffset.UTC));
        return new SesionResponse(acceso, "Bearer", expiraEn, crudo, usuario.getId(), sesion);
    }
}
