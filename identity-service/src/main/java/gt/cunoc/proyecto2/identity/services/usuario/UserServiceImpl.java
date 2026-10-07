package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioListaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Perfiles, altas de administradores y estados de cuenta.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Duration ACTIVACION_DURACION = Duration.ofHours(48);
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfiles;
    private final RolRepository roles;
    private final CarteraRepository carteras;
    private final TokenRecuperacionRepository activaciones;
    private final TokenRefrescoRepository refrescos;
    private final PasswordEncoder claves;
    private final TokenOpacoService opacos;
    private final AlmacenFotos fotos;
    private final UsuarioEventPublisher publicador;
    private final String urlFrontend;

    public UserServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            CarteraRepository carteras,
            TokenRecuperacionRepository activaciones,
            TokenRefrescoRepository refrescos,
            PasswordEncoder claves,
            TokenOpacoService opacos,
            AlmacenFotos fotos,
            UsuarioEventPublisher publicador,
            @Value("${FRONTEND_URL:http://localhost:5173}") String urlFrontend) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
        this.activaciones = activaciones;
        this.refrescos = refrescos;
        this.claves = claves;
        this.opacos = opacos;
        this.fotos = fotos;
        this.publicador = publicador;
        this.urlFrontend = urlFrontend;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse perfil(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);

        Perfil perfil = perfiles.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("PERFIL_NO_EXISTE", "El perfil no existe"));
        return aPerfil(usuario, perfil);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDetalleResponse detalle(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        Perfil perfil = perfiles.findById(usuarioId).orElse(null);
        return aDetalle(usuario, perfil);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioListaResponse> listar(String rol, String estado, String q, Pageable paginado) {
        return usuarios.buscar(vacio(rol), vacio(estado), vacio(q), paginado).map(usuario -> {
            Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
            String nombre = perfil == null ? usuario.getCorreo()
                    : perfil.getNombres() + " " + perfil.getApellidos();
            return new UsuarioListaResponse(
                    usuario.getId(), usuario.getCorreo(), nombre,
                    perfil == null ? null : perfil.getNit(),
                    codigos(usuario), usuario.getEstado(),
                    usuario.getSucursalId(), usuario.getUltimoAcceso());
        });
    }

    @Override
    @Transactional
    public UsuarioDetalleResponse crearAdministradorSistema(AdministradorSistemaRequest solicitud) {
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
        usuario.setClaveCifrada(claves.encode(UUID.randomUUID().toString()));
        usuario.setEstado("ACTIVO");
        usuario.setRoles(roles.findAllById(Set.of(Roles.ADMIN_SISTEMA, Roles.CLIENTE)).stream().collect(Collectors.toSet()));
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

        String crudo = opacos.generar();
        TokenRecuperacion activacion = new TokenRecuperacion();
        activacion.setUsuarioId(usuario.getId());
        activacion.setTokenHash(opacos.calcularHash(crudo));
        activacion.setExpiraEn(Instant.now().plus(ACTIVACION_DURACION));
        activaciones.save(activacion);

        String nombre = solicitud.nombres() + " " + solicitud.apellidos();
        publicador.publicarCuentaCreada(
                usuario.getId(), correo, nombre,
                List.of(Roles.ADMIN_SISTEMA, Roles.CLIENTE),
                urlFrontend + "/restablecer?token=" + crudo,
                LocalDate.now());
        publicador.publicarClienteRegistrado(
                usuario.getId(), solicitud.nit(), nombre, correo,
                LocalDate.now());
        return aDetalle(usuario, perfil);
    }

    @Override
    @Transactional
    public PerfilResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest solicitud) {
        Usuario usuario = buscar(usuarioId);
        Perfil perfil = perfiles.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("PERFIL_NO_EXISTE", "El perfil no existe"));
        if (solicitud.nombres() != null) {
            perfil.setNombres(solicitud.nombres());
        }
        if (solicitud.apellidos() != null) {
            perfil.setApellidos(solicitud.apellidos());
        }
        if (solicitud.nit() != null && !solicitud.nit().equals(perfil.getNit())) {
            if (perfiles.existsByNit(solicitud.nit())) {
                throw new ConflictoException("NIT_EN_USO", "El NIT ya está registrado");
            }
            perfil.setNit(solicitud.nit());
        }
        if (solicitud.telefono() != null) {
            perfil.setTelefono(solicitud.telefono());
        }
        if (solicitud.direccion() != null) {
            perfil.setDireccion(solicitud.direccion());
        }
        if (solicitud.fotoUrl() != null) {
            if (!fotos.esDelBucket(solicitud.fotoUrl())) {
                throw new SolicitudInvalidaException("FOTO_INVALIDA", "La foto debe subirse con /identity/archivos/fotos");
            }
            perfil.setFotoUrl(solicitud.fotoUrl());
        }
        if (usuario.getChoferId() != null
                && (perfil.getFotoUrl() == null || perfil.getFotoUrl().isBlank())) {
            throw new ReglaNegocioException("FOTO_OBLIGATORIA", "La foto es obligatoria para los choferes");
        }
        perfiles.save(perfil);
        publicador.publicarPerfilActualizado(
                usuario.getId(), perfil.getNit(), perfil.getNombres(), perfil.getApellidos(),
                usuario.getCorreo(), perfil.getTelefono(), perfil.getFotoUrl(), usuario.getChoferId(),
                LocalDate.now());
        return aPerfil(usuario, perfil);
    }

    @Override
    @Transactional
    public UsuarioDetalleResponse cambiarEstado(UUID usuarioId, UserStatusRequest solicitud) {
        Usuario usuario = buscar(usuarioId);
        if (usuario.getSucursalId() != null || usuario.getChoferId() != null) {
            throw new ReglaNegocioException("GESTIONAR_EN_FLEET", "El personal y los choferes se desactivan en Fleet");
        }
        boolean esUltimoAdmin = "ACTIVO".equals(usuario.getEstado())
                && !"ACTIVO".equals(solicitud.estado())
                && usuario.getRoles().stream().anyMatch(rol -> Roles.ADMIN_SISTEMA.equals(rol.getCodigo()))
                && usuarios.countByEstadoAndRoles_Codigo("ACTIVO", Roles.ADMIN_SISTEMA) <= 1;
        if (esUltimoAdmin) {
            throw new ReglaNegocioException(
                    "ULTIMO_ADMIN_SISTEMA",
                    "No se puede desactivar al último administrador del sistema activo");
        }
        usuario.setEstado(solicitud.estado());
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuarioId).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
        return aDetalle(usuario, perfiles.findById(usuarioId).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminContactoResponse> administradoresDe(UUID sucursalId) {
        return usuarios.findBySucursalIdAndRoles_Codigo(sucursalId, Roles.ADMIN_SUCURSAL).stream()
                .filter(admin -> "ACTIVO".equals(admin.getEstado()))
                .map(admin -> {
                    Perfil perfil = perfiles.findById(admin.getId()).orElse(null);
                    String nombre = perfil == null ? admin.getCorreo()
                            : perfil.getNombres() + " " + perfil.getApellidos();
                    return new AdminContactoResponse(admin.getId(), nombre, admin.getCorreo());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContactoResponse contactoDe(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        Perfil perfil = perfiles.findById(usuarioId).orElse(null);
        String nombre = perfil == null ? usuario.getCorreo()
                : perfil.getNombres() + " " + perfil.getApellidos();
        return new ContactoResponse(
                usuario.getId(), nombre, usuario.getCorreo(),
                perfil == null ? null : perfil.getTelefono());
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
    }

    private String vacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private Set<String> codigos(Usuario usuario) {
        return usuario.getRoles().stream()
                .map(Rol::getCodigo)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private PerfilResponse aPerfil(Usuario usuario, Perfil perfil) {
        return new PerfilResponse(
                usuario.getId(), usuario.getCorreo(),
                perfil.getNombres(), perfil.getApellidos(), perfil.getNit(), perfil.getDpi(),
                perfil.getTelefono(), perfil.getDireccion(), perfil.getFotoUrl(),
                codigos(usuario), usuario.getSucursalId(), usuario.getChoferId());
    }

    private UsuarioDetalleResponse aDetalle(Usuario usuario, Perfil perfil) {
        return new UsuarioDetalleResponse(
                usuario.getId(), usuario.getCorreo(),
                perfil == null ? null : perfil.getNombres(),
                perfil == null ? null : perfil.getApellidos(),
                perfil == null ? null : perfil.getNit(),
                perfil == null ? null : perfil.getDpi(),
                perfil == null ? null : perfil.getTelefono(),
                perfil == null ? null : perfil.getDireccion(),
                perfil == null ? null : perfil.getFotoUrl(),
                codigos(usuario), usuario.getSucursalId(), usuario.getChoferId(),
                usuario.getEstado(), usuario.getUltimoAcceso());
    }
}
