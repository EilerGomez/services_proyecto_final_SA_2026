package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.TokenRefrescoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantiene sucursales, roles y sesiones de las cuentas de personal. */
@Service
public class SincronizacionPersonalService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacionPersonalService.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final TokenRefrescoRepository refrescos;
    private final PerfilRepository perfiles;
    private final UsuarioEventPublisher publicador;

    public SincronizacionPersonalService(UsuarioRepository usuarios, RolRepository roles,
                                         TokenRefrescoRepository refrescos, PerfilRepository perfiles,
                                         UsuarioEventPublisher publicador) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.refrescos = refrescos;
        this.perfiles = perfiles;
        this.publicador = publicador;
    }

    @Transactional
    public void desactivarChofer(UUID choferId, UUID sucursalId) {
        Usuario usuario = usuarios.findByChoferId(choferId).orElse(null);

        if (usuario == null) {
            return;
        }
        if (Objects.equals(sucursalId, usuario.getSucursalId())) {
            usuario.setSucursalId(null);
        }

        usuario.getRoles().removeIf(rol -> Roles.CHOFER.equals(rol.getCodigo()));
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }

    /**
     * PersonalSucursalAsignado (alta con cuenta existente por R5b, traslado o reintegro). Si el rol es
     * nuevo para la cuenta publica RolAsignado para avisarle por correo. En las altas por R5 (cuenta
     * nueva) el rol ya está puesto y basta la bienvenida.
     * Última defensa: si entre la validación de R5b y este evento la cuenta pasó a ser administrador del
     * sistema (o es de un chofer), no recibe el rol de personal.
     */
    @Transactional
    public void asignarPersonal(UUID usuarioId, UUID sucursalId, String rolCodigo, String sucursalNombre) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        if (usuario == null) {
            return;
        }
        Optional<NegocioException> impedimento = ReglasAsignacionRol.impedimentoPorTipoDeCuenta(usuario);
        if (impedimento.isPresent()) {
            log.warn("PersonalSucursalAsignado ignorado para el usuario {}: {}", usuarioId,
                    impedimento.get().getMessage());
            return;
        }

        usuario.setSucursalId(sucursalId);
        Rol rol = roles.findById(rolCodigo).orElse(null);
        boolean rolNuevo = false;

        if (rol != null && (Roles.ADMIN_SUCURSAL.equals(rol.getCodigo()) || Roles.CAJERO.equals(rol.getCodigo()))) {
            rolNuevo = usuario.getRoles().stream().noneMatch(actual -> rol.getCodigo().equals(actual.getCodigo()));
            usuario.getRoles().add(rol);
        }
        usuarios.save(usuario);
        if (rolNuevo) {
            publicador.publicarRolAsignado(usuario.getId(), usuario.getCorreo(), nombreDe(usuario),
                    rol.getCodigo(), sucursalId, sucursalNombre, LocalDate.now());
        }
    }

    private String nombreDe(Usuario usuario) {
        Perfil perfil = perfiles.findById(usuario.getId()).orElse(null);
        return perfil == null ? usuario.getCorreo() : perfil.getNombres() + " " + perfil.getApellidos();
    }

    @Transactional
    public void desactivarPersonal(UUID usuarioId, UUID sucursalId, String rolCodigo) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        if (usuario == null || !Objects.equals(sucursalId, usuario.getSucursalId())) {
            return;
        }
        usuario.setSucursalId(null);
        usuario.getRoles().removeIf(rol -> rolCodigo.equals(rol.getCodigo()));
        usuarios.save(usuario);
        refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId()).forEach(token -> {
            token.setRevocadoEn(Instant.now());
            refrescos.save(token);
        });
    }
}
