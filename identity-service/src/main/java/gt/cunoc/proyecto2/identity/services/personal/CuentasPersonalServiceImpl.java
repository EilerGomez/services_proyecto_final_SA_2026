package gt.cunoc.proyecto2.identity.services.personal;


import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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

    private static final Duration ACTIVACION_DURACION = Duration.ofHours(48);

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final CarteraRepository carteras;

    private final SolicitudCuentaRepository solicitudes;

    private final TokenRecuperacionRepository activaciones;

    private final PasswordEncoder claves;

    private final TokenOpacoService opacos;

    private final AlmacenFotos fotos;

    private final PublicadorEventos publicador;

    private final String urlFrontend;

    public CuentasPersonalServiceImpl(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            CarteraRepository carteras,
            SolicitudCuentaRepository solicitudes,
            TokenRecuperacionRepository activaciones,
            PasswordEncoder claves,
            TokenOpacoService opacos,
            AlmacenFotos fotos,
            PublicadorEventos publicador,
            @Value("${FRONTEND_URL:http://localhost:5173}") String urlFrontend) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
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
            Usuario existente = usuarios.findById(previa.getUsuarioId())
                    .orElseThrow(() -> new ReglaNegocioException("CUENTA_INCONSISTENTE", "La solicitud ya existe sin usuario"));
            Perfil perfil = perfiles.findById(existente.getId()).orElse(null);
            String nombrePrevio = perfil == null ? existente.getCorreo()
                    : perfil.getNombres() + " " + perfil.getApellidos();
            return new CuentaPersonalResponse(existente.getId(), existente.getCorreo(), nombrePrevio, false);
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
        Cartera cartera = new Cartera();
        cartera.setUsuarioId(usuario.getId());
        cartera.setSaldo(BigDecimal.ZERO);
        carteras.save(cartera);
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
        publicador.publicar(
                "Usuario", usuario.getId(), "CuentaCreada",
                new CuentaCreadaPayload(usuario.getId(), correo, nombre, codigos,
                        urlFrontend + "/restablecer?token=" + crudo),
                LocalDate.now());
        publicador.publicar(
                "Usuario", usuario.getId(), "ClienteRegistrado",
                new ClienteRegistradoPayload(usuario.getId(), solicitud.nit(), nombre, correo),
                LocalDate.now());
        return new CuentaPersonalResponse(usuario.getId(), correo, nombre, true);
    }
}
