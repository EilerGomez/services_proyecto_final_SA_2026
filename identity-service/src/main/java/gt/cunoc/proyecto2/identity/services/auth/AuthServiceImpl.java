package gt.cunoc.proyecto2.identity.services.auth;


import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioSesion;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.RecuperacionContrasenaPayload;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.TokenRefresco;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.IdentidadUsuarioFactory;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Sesión con bloqueo por intentos, registro público y tokens de sesión.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_INTENTOS = 5;

    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private static final Duration REFRESCO_DURACION = Duration.ofDays(7);

    private static final Duration RECUPERACION_DURACION = Duration.ofMinutes(30);

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final CarteraRepository carteras;

    private final TokenRefrescoRepository refrescos;

    private final TokenRecuperacionRepository recuperaciones;

    private final PasswordEncoder claves;

    private final ServicioJwt jwt;

    private final long minutosAcceso;

    private final IdentidadUsuarioFactory fabrica;

    private final TokenOpacoService opacos;

    private final PublicadorEventos publicador;

    private final UserService servicioUsuarios;

    private final String urlFrontend;

    public AuthServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            CarteraRepository carteras,
            TokenRefrescoRepository refrescos,
            TokenRecuperacionRepository recuperaciones,
            PasswordEncoder claves,
            ServicioJwt jwt,
            @Value("${app.jwt.duracion-minutos:60}") long minutosAcceso,
            IdentidadUsuarioFactory fabrica,
            TokenOpacoService opacos,
            PublicadorEventos publicador,
            UserService servicioUsuarios,
            @Value("${FRONTEND_URL:http://localhost:5173}") String urlFrontend) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
        this.refrescos = refrescos;
        this.recuperaciones = recuperaciones;
        this.claves = claves;
        this.jwt = jwt;
        this.minutosAcceso = minutosAcceso;
        this.fabrica = fabrica;
        this.opacos = opacos;
        this.publicador = publicador;
        this.servicioUsuarios = servicioUsuarios;
        this.urlFrontend = urlFrontend;
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
        publicador.publicar(
                "Usuario", usuario.getId(), "CuentaCreada",
                new CuentaCreadaPayload(usuario.getId(), correo, nombre, List.of(Roles.CLIENTE), null),
                LocalDate.now());
        publicador.publicar(
                "Usuario", usuario.getId(), "ClienteRegistrado",
                new ClienteRegistradoPayload(usuario.getId(), solicitud.nit(), nombre, correo),
                LocalDate.now());
        return new RegistroResponse(usuario.getId(), correo, Set.of(Roles.CLIENTE));
    }

    @Override
    @Transactional
    public SesionResponse iniciarSesion(InicioSesionRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        Usuario usuario = usuarios.findByCorreo(correo).orElse(null);
        if (usuario == null || !claves.matches(solicitud.contrasena(), usuario.getClaveCifrada())) {
            registrarFallo(usuario);
            throw new NoAutenticadoException("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
        }
        if ("BLOQUEADO".equals(usuario.getEstado())) {
            if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(Instant.now())) {
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
        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(Instant.now());
        usuarios.save(usuario);
        return crearTokens(usuario);
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
        publicador.publicar(
                "Usuario", usuario.getId(), "RecuperacionContrasenaSolicitada",
                new RecuperacionContrasenaPayload(
                        usuario.getId(), correo, nombre,
                        urlFrontend + "/restablecer?token=" + crudo, token.getExpiraEn()),
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
        usuario.setBloqueadoHasta(null);
        if ("BLOQUEADO".equals(usuario.getEstado())) {
            usuario.setEstado("ACTIVO");
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
        if (!claves.matches(solicitud.contrasenaActual(), usuario.getClaveCifrada())) {
            throw new ReglaNegocioException("CONTRASENA_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
        }
        usuario.setClaveCifrada(claves.encode(solicitud.nuevaContrasena()));
        usuarios.save(usuario);
    }

    private void registrarFallo(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            usuario.setEstado("BLOQUEADO");
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO));
            usuario.setIntentosFallidos(0);
        } else {
            usuario.setIntentosFallidos(intentos);
        }
        usuarios.save(usuario);
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
