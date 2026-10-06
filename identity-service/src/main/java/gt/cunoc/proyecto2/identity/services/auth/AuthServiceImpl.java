package gt.cunoc.proyecto2.identity.services.auth;


import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
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
        this.fabrica = fabrica;
        this.opacos = opacos;
        this.publicador = publicador;
        this.servicioUsuarios = servicioUsuarios;
        this.urlFrontend = urlFrontend;
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
        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new ProhibidoException("CUENTA_INACTIVA", "La cuenta no está activa");
        }
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(Instant.now())) {
            throw new ProhibidoException("CUENTA_BLOQUEADA", "La cuenta está bloqueada temporalmente");
        }
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(Instant.now());
        usuarios.save(usuario);
        return crearTokens(usuario);
    }

    @Override
    @Transactional
    public UserResponse registrar(RegistroRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase();
        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException("CORREO_DUPLICADO", "El correo ya está registrado");
        }
        if (perfiles.existsByNit(solicitud.nit())) {
            throw new ConflictoException("NIT_DUPLICADO", "El NIT ya está registrado");
        }
        if (perfiles.existsByDpi(solicitud.dpi())) {
            throw new ConflictoException("DPI_DUPLICADO", "El DPI ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setClaveCifrada(claves.encode(solicitud.contrasena()));
        usuario.setEstado("ACTIVO");
        Rol cliente = roles.findById(Roles.CLIENTE)
                .orElseThrow(() -> new ReglaNegocioException("ROL_FALTANTE", "No existe el rol CLIENTE"));
        usuario.getRoles().add(cliente);
        usuarios.save(usuario);
        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setNombres(solicitud.nombres());
        perfil.setApellidos(solicitud.apellidos());
        perfil.setNit(solicitud.nit());
        perfil.setDpi(solicitud.dpi());
        perfil.setTelefono(solicitud.telefono());
        perfil.setDireccion(solicitud.direccion());
        perfiles.save(perfil);
        Cartera cartera = new Cartera();
        cartera.setUsuarioId(usuario.getId());
        cartera.setSaldo(BigDecimal.ZERO);
        carteras.save(cartera);
        String nombre = solicitud.nombres() + " " + solicitud.apellidos();
        publicador.publicar(
                "Cuenta",
                usuario.getId(),
                "CuentaCreada",
                new CuentaCreadaPayload(usuario.getId(), correo, nombre, List.of(Roles.CLIENTE), null),
                LocalDate.now());
        publicador.publicar(
                "Cliente",
                usuario.getId(),
                "ClienteRegistrado",
                new ClienteRegistradoPayload(usuario.getId(), solicitud.nit(), nombre, correo),
                LocalDate.now());
        return servicioUsuarios.ver(usuario.getId());
    }

    @Override
    @Transactional
    public SesionResponse renovarSesion(TokenRefrescoRequest solicitud) {
        TokenRefresco guardado = refrescos.findByTokenHash(opacos.calcularHash(solicitud.tokenRefresco()))
                .orElseThrow(() -> new NoAutenticadoException("REFRESCO_INVALIDO", "La sesión no es válida"));
        if (guardado.getRevocadoEn() != null || guardado.getExpiraEn().isBefore(Instant.now())) {
            throw new NoAutenticadoException("REFRESCO_VENCIDO", "La sesión expiró, vuelva a iniciar sesión");
        }
        Usuario usuario = usuarios.findById(guardado.getUsuarioId())
                .orElseThrow(() -> new NoAutenticadoException("REFRESCO_INVALIDO", "La sesión no es válida"));
        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new ProhibidoException("CUENTA_INACTIVA", "La cuenta no está activa");
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
                "Cuenta",
                usuario.getId(),
                "RecuperacionContrasenaSolicitada",
                new RecuperacionContrasenaPayload(
                        usuario.getId(),
                        correo,
                        nombre,
                        urlFrontend + "/recuperar?token=" + crudo,
                        token.getExpiraEn()),
                LocalDate.now());
    }

    @Override
    @Transactional
    public void restablecerContrasena(RestablecimientoRequest solicitud) {
        TokenRecuperacion token = recuperaciones.findByTokenHash(opacos.calcularHash(solicitud.token()))
                .orElseThrow(() -> new SolicitudInvalidaException("TOKEN_INVALIDO", "El enlace no es válido"));
        if (token.getUsadoEn() != null || token.getExpiraEn().isBefore(Instant.now())) {
            throw new SolicitudInvalidaException("TOKEN_VENCIDO", "El enlace expiró, solicite otro");
        }
        Usuario usuario = usuarios.findById(token.getUsuarioId())
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        usuario.setClaveCifrada(claves.encode(solicitud.nuevaContrasena()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarios.save(usuario);
        token.setUsadoEn(Instant.now());
        recuperaciones.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse miPerfil(UUID usuarioId) {
        return servicioUsuarios.ver(usuarioId);
    }

    @Override
    @Transactional
    public void cambiarContrasena(UUID usuarioId, CambioContrasenaRequest solicitud) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        if (!claves.matches(solicitud.contrasenaActual(), usuario.getClaveCifrada())) {
            throw new NoAutenticadoException("CONTRASENA_INCORRECTA", "La contraseña actual no es correcta");
        }
        usuario.setClaveCifrada(claves.encode(solicitud.nuevaContrasena()));
        usuarios.save(usuario);
    }

    private void registrarFallo(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO));
            usuario.setIntentosFallidos(0);
        }
        usuarios.save(usuario);
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
        Set<String> roles = actual.roles().stream().sorted().collect(Collectors.toSet());
        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        UserResponse respuesta = new UserResponse(
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getEstado(),
                roles,
                perfil == null ? null : perfil.getNombres(),
                perfil == null ? null : perfil.getApellidos(),
                perfil == null ? null : perfil.getNit(),
                perfil == null ? null : perfil.getTelefono());
        return new SesionResponse(acceso, "Bearer", 3600L, crudo, usuario.getId(), respuesta);
    }
}
