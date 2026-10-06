package gt.cunoc.proyecto2.identity.services.usuario;


import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminCreateUserRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.RolesRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.PerfilActualizadoPayload;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Altas, perfiles, roles y estados de usuario.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final CarteraRepository carteras;

    private final PasswordEncoder claves;

    private final PublicadorEventos publicador;

    public UserServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            CarteraRepository carteras,
            PasswordEncoder claves,
            PublicadorEventos publicador) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
        this.claves = claves;
        this.publicador = publicador;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse ver(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        return aRespuesta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listar(Pageable paginado) {
        return usuarios.findAll(paginado).map(this::aRespuesta);
    }

    @Override
    @Transactional
    public UserResponse crear(AdminCreateUserRequest solicitud) {
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
        usuario.setRoles(validarRoles(solicitud.roles()));
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
        List<String> codigos = usuario.getRoles().stream()
                .map(Rol::getCodigo)
                .sorted()
                .toList();
        publicador.publicar(
                "Cuenta",
                usuario.getId(),
                "CuentaCreada",
                new CuentaCreadaPayload(usuario.getId(), correo, nombre, codigos, null),
                LocalDate.now());
        publicador.publicar(
                "Cliente",
                usuario.getId(),
                "ClienteRegistrado",
                new ClienteRegistradoPayload(usuario.getId(), solicitud.nit(), nombre, correo),
                LocalDate.now());
        return aRespuesta(usuario);
    }

    @Override
    @Transactional
    public UserResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest solicitud) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        Perfil perfil = perfiles.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("PERFIL_NO_EXISTE", "El perfil no existe"));
        if (solicitud.nombres() != null) {
            perfil.setNombres(solicitud.nombres());
        }
        if (solicitud.apellidos() != null) {
            perfil.setApellidos(solicitud.apellidos());
        }
        if (solicitud.telefono() != null) {
            perfil.setTelefono(solicitud.telefono());
        }
        if (solicitud.direccion() != null) {
            perfil.setDireccion(solicitud.direccion());
        }
        if (solicitud.fotoUrl() != null) {
            perfil.setFotoUrl(solicitud.fotoUrl());
        }
        perfiles.save(perfil);
        publicador.publicar(
                "Cliente",
                usuario.getId(),
                "PerfilActualizado",
                new PerfilActualizadoPayload(
                        usuario.getId(),
                        perfil.getNit(),
                        perfil.getNombres(),
                        perfil.getApellidos(),
                        usuario.getCorreo(),
                        perfil.getTelefono(),
                        perfil.getFotoUrl(),
                        usuario.getChoferId()),
                LocalDate.now());
        return aRespuesta(usuario);
    }

    @Override
    @Transactional
    public UserResponse asignarRoles(UUID usuarioId, RolesRequest solicitud) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
        usuario.setRoles(validarRoles(solicitud.roles()));
        usuarios.save(usuario);
        return aRespuesta(usuario);
    }

    @Override
    @Transactional
    public UserResponse cambiarEstado(UUID usuarioId, UserStatusRequest solicitud) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("USUARIO_NO_EXISTE", "El usuario no existe"));
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
        return aRespuesta(usuario);
    }

    private Set<Rol> validarRoles(Set<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            throw new ReglaNegocioException("ROLES_VACIOS", "Debe asignar al menos un rol");
        }
        Set<Rol> encontrados = roles.findAllById(codigos).stream().collect(Collectors.toSet());
        if (encontrados.size() != codigos.size()) {
            throw new ReglaNegocioException("ROL_INEXISTENTE", "Uno de los roles no existe");
        }
        return encontrados;
    }

    private UserResponse aRespuesta(Usuario usuario) {
        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        Set<String> codigos = usuario.getRoles().stream()
                .map(Rol::getCodigo)
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserResponse(
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getEstado(),
                codigos,
                perfil == null ? null : perfil.getNombres(),
                perfil == null ? null : perfil.getApellidos(),
                perfil == null ? null : perfil.getNit(),
                perfil == null ? null : perfil.getTelefono());
    }
}
