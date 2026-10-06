package gt.cunoc.proyecto2.identity.services.personal;


import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.SolicitudCuentaRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * R5 idempotente: la llave es {@code solicitudId} generado en Fleet.
 */
@Service
public class CuentasPersonalServiceImpl implements CuentasPersonalService {

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final CarteraRepository carteras;

    private final SolicitudCuentaRepository solicitudes;

    private final PasswordEncoder claves;

    private final PublicadorEventos publicador;

    private final UserService servicioUsuarios;

    public CuentasPersonalServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            CarteraRepository carteras,
            SolicitudCuentaRepository solicitudes,
            PasswordEncoder claves,
            PublicadorEventos publicador,
            UserService servicioUsuarios) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
        this.solicitudes = solicitudes;
        this.claves = claves;
        this.publicador = publicador;
        this.servicioUsuarios = servicioUsuarios;
    }

    @Override
    @Transactional
    public UserResponse crear(CuentaPersonalRequest solicitud) {
        SolicitudCuentaPersonal previa = solicitudes.findById(solicitud.solicitudId()).orElse(null);
        if (previa != null) {
            return servicioUsuarios.ver(previa.getUsuarioId());
        }
        boolean esChofer = Roles.CHOFER.equals(solicitud.rolCodigo());
        if (esChofer && (solicitud.fotoUrl() == null || solicitud.fotoUrl().isBlank())) {
            throw new SolicitudInvalidaException("FOTO_REQUERIDA", "La foto es obligatoria para los choferes");
        }
        String correo = solicitud.correo().trim().toLowerCase();
        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException("CORREO_DUPLICADO", "El correo ya está registrado");
        }
        if (perfiles.existsByDpi(solicitud.dpi())) {
            throw new ConflictoException("DPI_DUPLICADO", "El DPI ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setClaveCifrada(claves.encode(UUID.randomUUID().toString()));
        usuario.setEstado("ACTIVO");
        usuario.setSucursalId(solicitud.sucursalId());
        if (esChofer) {
            usuario.setChoferId(solicitud.solicitudId());
        }
        Set<Rol> rolesCuenta = roles.findAllById(Set.of(solicitud.rolCodigo(), Roles.CLIENTE)).stream()
                .collect(Collectors.toSet());
        if (rolesCuenta.size() != 2) {
            throw new ReglaNegocioException("ROL_FALTANTE", "No existen los roles solicitados");
        }
        usuario.setRoles(rolesCuenta);
        usuarios.save(usuario);
        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setNombres(solicitud.nombres());
        perfil.setApellidos(solicitud.apellidos());
        perfil.setNit(solicitud.nit());
        perfil.setDpi(solicitud.dpi());
        perfil.setTelefono(solicitud.telefono());
        perfil.setDireccion(solicitud.direccion());
        perfil.setFotoUrl(solicitud.fotoUrl());
        perfiles.save(perfil);
        Cartera cartera = new Cartera();
        cartera.setUsuarioId(usuario.getId());
        cartera.setSaldo(BigDecimal.ZERO);
        carteras.save(cartera);
        SolicitudCuentaPersonal registro = new SolicitudCuentaPersonal();
        registro.setSolicitudId(solicitud.solicitudId());
        registro.setUsuarioId(usuario.getId());
        registro.setRolCodigo(solicitud.rolCodigo());
        registro.setSucursalId(solicitud.sucursalId());
        solicitudes.save(registro);
        String nombre = solicitud.nombres() + " " + solicitud.apellidos();
        List<String> codigos = rolesCuenta.stream().map(Rol::getCodigo).sorted().toList();
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
        return servicioUsuarios.ver(usuario.getId());
    }
}
