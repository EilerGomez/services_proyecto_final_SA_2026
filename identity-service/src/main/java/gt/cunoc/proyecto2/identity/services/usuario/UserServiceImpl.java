package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.CompletarPerfilRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.EstadoCuentaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioAsignableResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioBusquedaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioListaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.PuestoAsignable;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import gt.cunoc.proyecto2.identity.services.personal.ReglasAsignacionRol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    @Transactional(readOnly = true)
    public UsuarioBusquedaResponse buscarPorCorreo(String correo, boolean motivoDetallado, PuestoAsignable puesto) {
        if (correo == null || correo.isBlank()) {
            throw new SolicitudInvalidaException("CORREO_REQUERIDO", "Escriba el correo de la cuenta");
        }
        Usuario usuario = usuarios.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE",
                        "No hay ninguna cuenta con ese correo"));
        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        boolean paraChofer = puesto == PuestoAsignable.CHOFER;
        Optional<String> motivo = ReglasAsignacionRol.impedimentoPara(puesto, usuario, Instant.now())
                .map(impedimento -> motivoVisible(impedimento, motivoDetallado, paraChofer));
        return new UsuarioBusquedaResponse(
                usuario.getId(), usuario.getCorreo(), nombreDe(usuario, perfil),
                perfil == null ? null : perfil.getFotoUrl(),
                motivo.isEmpty(), motivo.orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioAsignableResponse> asignables(PuestoAsignable puesto) {
        Instant ahora = Instant.now();
        List<Usuario> candidatos = usuarios.findByEstadoOrderByCorreoAsc("ACTIVO").stream()
                .filter(usuario -> ReglasAsignacionRol.impedimentoPara(puesto, usuario, ahora).isEmpty())
                .toList();
        Map<UUID, Perfil> perfilPorUsuario = perfiles.findAllById(candidatos.stream().map(Usuario::getId).toList())
                .stream().collect(Collectors.toMap(Perfil::getUsuarioId, perfil -> perfil));
        return candidatos.stream().map(usuario -> {
            Perfil perfil = perfilPorUsuario.get(usuario.getId());
            return new UsuarioAsignableResponse(usuario.getId(), usuario.getCorreo(), nombreDe(usuario, perfil),
                    perfil == null ? null : perfil.getFotoUrl());
        }).toList();
    }

    @Override
    @Transactional
    public UsuarioDetalleResponse hacerAdministradorSistema(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        ReglasAsignacionRol.impedimentoParaAdminSistema(usuario, Instant.now()).ifPresent(impedimento -> {
            throw impedimento;
        });
        Rol rol = roles.findById(Roles.ADMIN_SISTEMA)
                .orElseThrow(() -> new ReglaNegocioException("ROL_FALTANTE", "No existen los roles solicitados"));
        usuario.getRoles().add(rol);
        usuarios.save(usuario);
        Perfil perfil = perfiles.findById(usuarioId).orElse(null);
        publicador.publicarRolAsignado(usuario.getId(), usuario.getCorreo(), nombreDe(usuario, perfil),
                Roles.ADMIN_SISTEMA, null, null, LocalDate.now());
        return aDetalle(usuario, perfil);
    }

    @Override
    @Transactional
    public PerfilResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest solicitud) {
        Usuario usuario = buscar(usuarioId);
        Perfil perfil = perfiles.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("PERFIL_NO_EXISTE", "El perfil no existe"));
        String fotoAnterior = perfil.getFotoUrl();
        if (solicitud.nombres() != null) {
            perfil.setNombres(solicitud.nombres());
        }
        if (solicitud.apellidos() != null) {
            perfil.setApellidos(solicitud.apellidos());
        }
        if (solicitud.nit() != null) {
            // NIT opcional: vacío o "CF" se guarda sin NIT (Sales lo trata como consumidor final)
            String nit = solicitud.nit().trim();
            nit = nit.isEmpty() || "CF".equalsIgnoreCase(nit) ? null : nit;
            if (nit != null && !nit.equals(perfil.getNit()) && perfiles.existsByNit(nit)) {
                throw new ConflictoException("NIT_EN_USO", "El NIT ya está registrado");
            }
            perfil.setNit(nit);
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
        // Foto reemplazada: la anterior se borra del almacenamiento cuando se confirma el cambio
        if (fotoAnterior != null && !fotoAnterior.equals(perfil.getFotoUrl())) {
            fotos.eliminarAlConfirmar(fotoAnterior);
        }
        return aPerfil(usuario, perfil);
    }

    @Override
    @Transactional
    public PerfilResponse completarPerfil(UUID usuarioId, CompletarPerfilRequest solicitud) {
        Usuario usuario = buscar(usuarioId);
        Perfil perfil = perfiles.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("PERFIL_NO_EXISTE", "El perfil no existe"));
        if (perfil.estaCompleto()) {
            throw new ConflictoException("PERFIL_YA_COMPLETO", "Tu perfil ya está completo");
        }
        String dpi = solicitud.dpi().trim();
        if (perfiles.existsByDpi(dpi)) {
            throw new ConflictoException("DPI_EN_USO", "El DPI ya está registrado");
        }
        // Sin NIT (vacío o CF) queda null: la columna es única y Sales lo trata como "CF"
        String nit = solicitud.nit() == null ? "" : solicitud.nit().trim().toUpperCase();
        if (nit.isEmpty() || "CF".equals(nit)) {
            nit = null;
        } else if (!nit.equals(perfil.getNit()) && perfiles.existsByNit(nit)) {
            throw new ConflictoException("NIT_EN_USO", "El NIT ya está registrado");
        }
        perfil.setDpi(dpi);
        perfil.setNit(nit);
        perfil.setTelefono(solicitud.telefono().trim());
        perfil.setDireccion(solicitud.direccion().trim());
        perfiles.save(perfil);

        // Las cuentas de Google no se publican como ClienteRegistrado al crearse: ahora ya puede comprar.
        String nombre = perfil.getNombres() + " " + perfil.getApellidos();
        publicador.publicarClienteRegistrado(usuario.getId(), nit, nombre, usuario.getCorreo(), LocalDate.now());
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
        // Sin fecha: un BLOQUEADO puesto aquí no vence (solo se quita activando la cuenta de nuevo).
        // Activar o desactivar también limpia un bloqueo por intentos fallidos.
        usuario.setBloqueadoHasta(null);
        usuario.setIntentosFallidos(0);
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

    @Override
    @Transactional(readOnly = true)
    public EstadoCuentaResponse estadoCuentaDe(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        String impedimento = ReglasAsignacionRol.impedimentoPorTipoDeCuenta(usuario)
                .map(RuntimeException::getMessage).orElse(null);
        return new EstadoCuentaResponse(usuario.getId(), usuario.getEstado(), usuario.getBloqueadoHasta(),
                impedimento);
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
    }

    /** Un admin de sucursal no necesita saber si la cuenta es de un admin del sistema o de un chofer. */
    /** Al admin de sucursal no se le dice si la cuenta es de un admin del sistema, de un chofer o de otro personal. */
    private static String motivoVisible(NegocioException impedimento, boolean detallado, boolean paraChofer) {
        boolean tipoDeCuenta = "ES_ADMIN_SISTEMA".equals(impedimento.getCodigo())
                || "ES_CHOFER".equals(impedimento.getCodigo())
                || "YA_ES_CHOFER".equals(impedimento.getCodigo())
                || "ES_PERSONAL_SUCURSAL".equals(impedimento.getCodigo());
        if (detallado || !tipoDeCuenta) {
            return impedimento.getMessage();
        }
        return paraChofer
                ? "Esta cuenta no se puede asignar como chofer"
                : "Esta cuenta no se puede asignar como personal de sucursal";
    }

    private String nombreDe(Usuario usuario, Perfil perfil) {
        return perfil == null ? usuario.getCorreo() : perfil.getNombres() + " " + perfil.getApellidos();
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
