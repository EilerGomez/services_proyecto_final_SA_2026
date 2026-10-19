package gt.cunoc.proyecto2.identity.services.personal;


import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
import gt.cunoc.proyecto2.identity.dtos.personal.VincularCuentaRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.SolicitudCuentaRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRecuperacionRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.comun.TokenOpacoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * R5 (cuenta nueva) y R5b (cuenta existente) idempotentes: la llave es {@code solicitudId} generado en Fleet.
 */
@Service
public class CuentasPersonalServiceImpl implements CuentasPersonalService {

    private static final Duration ACTIVACION_DURACION = Duration.ofHours(48);

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final SolicitudCuentaRepository solicitudes;

    private final TokenRecuperacionRepository activaciones;

    private final PasswordEncoder claves;

    private final TokenOpacoService opacos;

    private final AlmacenFotos fotos;

    private final UsuarioEventPublisher publicador;

    private final String urlFrontend;

    public CuentasPersonalServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            SolicitudCuentaRepository solicitudes,
            TokenRecuperacionRepository activaciones,
            PasswordEncoder claves,
            TokenOpacoService opacos,
            AlmacenFotos fotos,
            UsuarioEventPublisher publicador,
            @Value("${FRONTEND_URL:http://localhost:5173}") String urlFrontend) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.solicitudes = solicitudes;
        this.activaciones = activaciones;
        this.claves = claves;
        this.opacos = opacos;
        this.fotos = fotos;
        this.publicador = publicador;
        this.urlFrontend = urlFrontend;
    }

    @Override
    @Transactional
    public CuentaPersonalResponse crear(CuentaPersonalRequest solicitud) {
        SolicitudCuentaPersonal previa = solicitudes.findById(solicitud.solicitudId()).orElse(null);
        if (previa != null) {
            return respuestaPrevia(previa);
        }
        boolean esChofer = Roles.CHOFER.equals(solicitud.rol());
        if (esChofer && solicitud.choferId() == null) {
            throw new SolicitudInvalidaException("CHOFER_SIN_ID", "El choferId es obligatorio para CHOFER");
        }
        if (esChofer && (solicitud.fotoUrl() == null || solicitud.fotoUrl().isBlank())) {
            throw new ReglaNegocioException("FOTO_OBLIGATORIA", "La foto es obligatoria para los choferes");
        }
        if (solicitud.fotoUrl() != null && !fotos.esDelBucket(solicitud.fotoUrl())) {
            throw new SolicitudInvalidaException("FOTO_INVALIDA", "La foto debe subirse con /identity/archivos/fotos");
        }
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
        usuario.setSucursalId(solicitud.sucursalId());
        if (esChofer) {
            usuario.setChoferId(solicitud.choferId());
        }
        Set<Rol> rolesCuenta = roles.findAllById(Set.of(solicitud.rol(), Roles.CLIENTE)).stream()
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
        SolicitudCuentaPersonal registro = new SolicitudCuentaPersonal();
        registro.setSolicitudId(solicitud.solicitudId());
        registro.setUsuarioId(usuario.getId());
        registro.setRolCodigo(solicitud.rol());
        registro.setSucursalId(solicitud.sucursalId());
        solicitudes.save(registro);
        String crudo = opacos.generar();
        TokenRecuperacion activacion = new TokenRecuperacion();
        activacion.setUsuarioId(usuario.getId());
        activacion.setTokenHash(opacos.calcularHash(crudo));
        activacion.setExpiraEn(Instant.now().plus(ACTIVACION_DURACION));
        activaciones.save(activacion);
        String nombre = solicitud.nombres() + " " + solicitud.apellidos();
        List<String> codigos = rolesCuenta.stream().map(Rol::getCodigo).sorted().toList();
        publicador.publicarCuentaCreada(
                usuario.getId(), correo, nombre, codigos,
                urlFrontend + "/restablecer?token=" + crudo,
                solicitud.sucursalNombre(),
                LocalDate.now());
        publicador.publicarClienteRegistrado(
                usuario.getId(), solicitud.nit(), nombre, correo,
                LocalDate.now());
        return new CuentaPersonalResponse(usuario.getId(), correo, nombre, true);
    }

    /**
     * R5b: la cuenta ya existe (la persona se registró antes). Solo VALIDA y registra la solicitud; no
     * cambia el rol. El rol y la sucursal se los pone el evento que Fleet publica en la misma transacción
     * en que guarda la fila (outbox): PersonalSucursalAsignado para el personal y ChoferCreado para el
     * chofer (su choferId es el solicitudId). Si Fleet revierte, la cuenta no queda con un rol sin respaldo.
     * Al recibir ese evento se publica RolAsignado (aviso por correo).
     *
     * <p>CHOFER: la foto es obligatoria. Si llega una foto nueva se guarda ya como foto de perfil (es la
     * foto de la persona; si Fleet revierte, solo queda la foto actualizada).
     */
    @Override
    @Transactional
    public CuentaPersonalResponse vincular(VincularCuentaRequest solicitud) {
        SolicitudCuentaPersonal previa = solicitudes.findById(solicitud.solicitudId()).orElse(null);
        if (previa != null) {
            boolean misma = previa.getUsuarioId().equals(solicitud.usuarioId())
                    && previa.getRolCodigo().equals(solicitud.rol())
                    && previa.getSucursalId().equals(solicitud.sucursalId());
            if (!misma) {
                throw new ConflictoException("SOLICITUD_YA_USADA",
                        "El solicitudId ya se usó con otros datos; genere uno nuevo");
            }
            // Si Fleet revirtió la vez anterior, la cuenta aún no tiene el puesto: se valida de nuevo
            Usuario previo = usuarios.findById(previa.getUsuarioId()).orElse(null);
            boolean yaTienePuesto = previo == null
                    || (esChofer(solicitud) ? previo.getChoferId() != null : ReglasAsignacionRol.esPersonal(previo));
            if (!yaTienePuesto) {
                impedimento(previo, solicitud).ifPresent(impedimento -> {
                    throw impedimento;
                });
            }
            return respuestaPrevia(previa);
        }
        // 422 (no 404): Fleet trata los 404 de otro servicio como error de configuración (503)
        Usuario usuario = usuarios.findById(solicitud.usuarioId())
                .orElseThrow(() -> new ReglaNegocioException("USUARIO_NO_EXISTE", "La cuenta indicada no existe"));
        impedimento(usuario, solicitud).ifPresent(impedimento -> {
            throw impedimento;
        });
        if (esChofer(solicitud)) {
            guardarFotoDeChofer(usuario, solicitud.fotoUrl());
        }

        SolicitudCuentaPersonal registro = new SolicitudCuentaPersonal();
        registro.setSolicitudId(solicitud.solicitudId());
        registro.setUsuarioId(usuario.getId());
        registro.setRolCodigo(solicitud.rol());
        registro.setSucursalId(solicitud.sucursalId());
        solicitudes.save(registro);

        return new CuentaPersonalResponse(usuario.getId(), usuario.getCorreo(), nombreDe(usuario), false);
    }

    private static boolean esChofer(VincularCuentaRequest solicitud) {
        return Roles.CHOFER.equals(solicitud.rol());
    }

    private static Optional<NegocioException> impedimento(Usuario usuario, VincularCuentaRequest solicitud) {
        return esChofer(solicitud)
                ? ReglasAsignacionRol.impedimentoParaChofer(usuario, Instant.now())
                : ReglasAsignacionRol.impedimentoParaPersonal(usuario, Instant.now());
    }

    /** Foto nueva (subida con /identity/archivos/fotos) o la que ya tiene el perfil; sin ninguna, 422. */
    private void guardarFotoDeChofer(Usuario usuario, String fotoUrl) {
        Perfil perfil = perfiles.findById(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioException("PERFIL_NO_EXISTE", "La cuenta no tiene perfil"));
        boolean conFotoNueva = fotoUrl != null && !fotoUrl.isBlank();
        if (!conFotoNueva) {
            if (perfil.getFotoUrl() == null || perfil.getFotoUrl().isBlank()) {
                throw new ReglaNegocioException("FOTO_OBLIGATORIA",
                        "La cuenta no tiene foto de perfil; suba la foto del chofer");
            }
            return;
        }
        if (!fotos.esDelBucket(fotoUrl)) {
            throw new SolicitudInvalidaException("FOTO_INVALIDA", "La foto debe subirse con /identity/archivos/fotos");
        }
        perfil.setFotoUrl(fotoUrl);
        perfiles.save(perfil);
    }

    /** Reintento de una solicitud ya procesada: devuelve la misma cuenta. */
    private CuentaPersonalResponse respuestaPrevia(SolicitudCuentaPersonal previa) {
        Usuario existente = usuarios.findById(previa.getUsuarioId())
                .orElseThrow(() -> new ReglaNegocioException("CUENTA_INCONSISTENTE", "La solicitud ya existe sin usuario"));
        return new CuentaPersonalResponse(existente.getId(), existente.getCorreo(), nombreDe(existente), false);
    }

    private String nombreDe(Usuario usuario) {
        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        return perfil == null ? usuario.getCorreo() : perfil.getNombres() + " " + perfil.getApellidos();
    }
}
