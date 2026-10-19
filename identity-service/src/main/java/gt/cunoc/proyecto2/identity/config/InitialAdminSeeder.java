package gt.cunoc.proyecto2.identity.config;


import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Crea el primer ADMIN_SISTEMA desde variables de entorno (solo si no existe ninguno).
 *
 * <p>Variables: {@code ADMIN_CORREO}, {@code ADMIN_CONTRASENA} y datos del perfil.
 *
 * <p>Roles: solo {@link #ROLES_ADMIN_INICIAL} (ADMIN_SISTEMA y CLIENTE). Los roles de sucursal
 * (ADMIN_SUCURSAL, CAJERO) y CHOFER no se dan aquí: se asignan desde el sistema, porque dependen
 * de una sucursal o de una licencia en fleet.
 *
 * <p>Publica CuentaCreada y ClienteRegistrado como cualquier otra cuenta: sin ClienteRegistrado, Sales no
 * tiene al admin en su vista de clientes y le responde CLIENTE_SIN_PERFIL al comprar.
 */
@Component
@Order(1)
public class InitialAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminSeeder.class);

    /** Roles con que nace el primer administrador. */
    static final List<String> ROLES_ADMIN_INICIAL = List.of(Roles.ADMIN_SISTEMA, Roles.CLIENTE);

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final PasswordEncoder claves;

    private final UsuarioEventPublisher publicador;

    private final String correo;

    private final String contrasena;

    private final String nombres;

    private final String apellidos;

    private final String nit;

    private final String dpi;

    private final String telefono;

    private final String direccion;

    public InitialAdminSeeder(
            UsuarioRepository usuarios,
            PerfilRepository perfiles,
            RolRepository roles,
            PasswordEncoder claves,
            UsuarioEventPublisher publicador,
            @Value("${ADMIN_CORREO:}") String correo,
            @Value("${ADMIN_CONTRASENA:}") String contrasena,
            @Value("${ADMIN_NOMBRES:Administrador}") String nombres,
            @Value("${ADMIN_APELLIDOS:Sistema}") String apellidos,
            @Value("${ADMIN_NIT:CF}") String nit,
            @Value("${ADMIN_DPI:0000000000000}") String dpi,
            @Value("${ADMIN_TELEFONO:00000000}") String telefono,
            @Value("${ADMIN_DIRECCION:Ciudad}") String direccion) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.claves = claves;
        this.publicador = publicador;
        this.correo = correo;
        this.contrasena = contrasena;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.nit = nit;
        this.dpi = dpi;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        long admins = usuarios.countByEstadoAndRoles_Codigo("ACTIVO", Roles.ADMIN_SISTEMA);
        if (admins > 0) {
            return;
        }
        if (correo.isBlank() || contrasena.isBlank()) {
            log.warn("Sin ADMIN_SISTEMA: defina ADMIN_CORREO e ADMIN_CONTRASENA");
            return;
        }
        if (usuarios.existsByCorreo(correo.trim().toLowerCase())) {
            log.warn("El correo del admin inicial ya existe");
            return;
        }
        Set<Rol> rolesAdmin = new HashSet<>(roles.findAllById(ROLES_ADMIN_INICIAL));
        if (rolesAdmin.size() != ROLES_ADMIN_INICIAL.size()) {
            log.error("No se creó el admin inicial: faltan roles {} en la tabla roles", ROLES_ADMIN_INICIAL);
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo.trim().toLowerCase());
        usuario.setClaveCifrada(claves.encode(contrasena));
        usuario.setEstado("ACTIVO");
        usuario.setRoles(rolesAdmin);
        usuarios.save(usuario);
        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setNombres(nombres);
        perfil.setApellidos(apellidos);
        perfil.setNit(nit);
        perfil.setDpi(dpi);
        perfil.setTelefono(telefono);
        perfil.setDireccion(direccion);
        perfiles.save(perfil);

        // Igual que una cuenta registrada: Wallet le abre la cartera, Notification le da la bienvenida
        // (ya tiene contraseña: sin enlace de activación) y Sales lo agrega a su vista de clientes.
        String nombre = nombres + " " + apellidos;
        publicador.publicarCuentaCreada(usuario.getId(), usuario.getCorreo(), nombre, ROLES_ADMIN_INICIAL, null,
                LocalDate.now());
        publicador.publicarClienteRegistrado(usuario.getId(), nit, nombre, usuario.getCorreo(), LocalDate.now());
        log.info("ADMIN_SISTEMA inicial creado: {}", usuario.getCorreo());
    }
}
