package gt.cunoc.proyecto2.identity.config;


import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
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

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Crea el primer ADMIN_SISTEMA desde variables de entorno (solo si no existe ninguno).
 *
 * <p>Variables: {@code INITIAL_ADMIN_CORREO}, {@code INITIAL_ADMIN_CONTRASENA} y datos del perfil.
 */
@Component
@Order(1)
public class InitialAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminSeeder.class);

    private final UsuarioRepository usuarios;

    private final PerfilRepository perfiles;

    private final RolRepository roles;

    private final CarteraRepository carteras;

    private final PasswordEncoder claves;

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
            CarteraRepository carteras,
            PasswordEncoder claves,
            @Value("${INITIAL_ADMIN_CORREO:}") String correo,
            @Value("${INITIAL_ADMIN_CONTRASENA:}") String contrasena,
            @Value("${INITIAL_ADMIN_NOMBRES:Administrador}") String nombres,
            @Value("${INITIAL_ADMIN_APELLIDOS:Sistema}") String apellidos,
            @Value("${INITIAL_ADMIN_NIT:CF}") String nit,
            @Value("${INITIAL_ADMIN_DPI:0000000000000}") String dpi,
            @Value("${INITIAL_ADMIN_TELEFONO:00000000}") String telefono,
            @Value("${INITIAL_ADMIN_DIRECCION:Ciudad}") String direccion) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.roles = roles;
        this.carteras = carteras;
        this.claves = claves;
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
            log.warn("Sin ADMIN_SISTEMA: defina INITIAL_ADMIN_CORREO e INITIAL_ADMIN_CONTRASENA");
            return;
        }
        if (usuarios.existsByCorreo(correo.trim().toLowerCase())) {
            log.warn("El correo del admin inicial ya existe");
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo.trim().toLowerCase());
        usuario.setClaveCifrada(claves.encode(contrasena));
        usuario.setEstado("ACTIVO");
        Set<Rol> todos = roles.findAll().stream().collect(Collectors.toSet());
        usuario.setRoles(todos);
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
        Cartera cartera = new Cartera();
        cartera.setUsuarioId(usuario.getId());
        cartera.setSaldo(BigDecimal.ZERO);
        carteras.save(cartera);
        log.info("ADMIN_SISTEMA inicial creado: {}", usuario.getCorreo());
    }
}
