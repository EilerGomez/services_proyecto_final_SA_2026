package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.evento.recibido.CuentaCreada;
import gt.cunoc.proyecto2.notification.evento.recibido.RolAsignado;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
 * Correos de cuentas con roles administrativos:
 * - Bienvenida de personal, choferes y administradores (dice el puesto y la sucursal).
 * - RolAsignado: a una cuenta existente le dieron un rol; debe cerrar sesión y volver a entrar
 *   para que su sesión tenga las nuevas opciones.
 * Las cuentas de cliente siguen con la bienvenida de siempre (EnvioCorreoService#enviarBienvenida).
 */
@Service
public class CorreoCuentasService {

    public static final String CUENTA_CREADA = "CUENTA_CREADA";
    public static final String ROL_ASIGNADO = "ROL_ASIGNADO";

    /* De mayor a menor: el que se menciona si la cuenta tiene varios. */
    private static final List<String> ROLES_CON_PUESTO = List.of("ADMIN_SISTEMA", "ADMIN_SUCURSAL", "CAJERO", "CHOFER");

    private static final String ASUNTO_BIENVENIDA = "Bienvenido a la plataforma";
    private static final String ASUNTO_ROL = "Tienes un nuevo rol en la plataforma";

    private final EnvioCorreoService correos;
    private final JsonMapper json;

    public CorreoCuentasService(EnvioCorreoService correos, JsonMapper json) {
        this.correos = correos;
        this.json = json;
    }

    /* CuentaCreada: bienvenida con el puesto (personal, chofer o admin) o la de cliente. */
    public void bienvenida(UUID eventoId, CuentaCreada cuenta) {
        Optional<String> puesto = puestoPrincipal(cuenta.roles());
        if (puesto.isEmpty()) {
            correos.enviarBienvenida(eventoId, cuenta.correo(), cuenta.nombre(), cuenta.urlActivacion());
            return;
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("rol", puesto.get());
        datos.put("sucursal", cuenta.sucursalNombre());
        datos.put("urlActivacion", cuenta.urlActivacion());
        String cuerpo = "Hola " + cuenta.nombre() + ",\n\n"
                + "Se creó tu cuenta en la plataforma como " + descripcion(puesto.get(), cuenta.sucursalNombre()) + ".\n"
                + (cuenta.urlActivacion() == null
                        ? "\nYa puedes iniciar sesión."
                        : "\nPara empezar, define tu contraseña aquí (el enlace vence en 48 horas):\n"
                                + cuenta.urlActivacion() + "\n\nLuego inicia sesión con tu correo y esa contraseña.");
        correos.enviarPlantilla(eventoId, cuenta.correo(), CUENTA_CREADA, ASUNTO_BIENVENIDA,
                json.writeValueAsString(datos), cuerpo);
    }

    /* RolAsignado: avisa el nuevo rol y que vuelva a iniciar sesión para ver los cambios. */
    public void rolAsignado(UUID eventoId, RolAsignado asignacion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("rol", asignacion.rol());
        datos.put("sucursalId", asignacion.sucursalId());
        datos.put("sucursal", asignacion.sucursalNombre());
        String cuerpo = "Hola " + asignacion.nombre() + ",\n\n"
                + "Se te asignó como " + descripcion(asignacion.rol(), asignacion.sucursalNombre())
                + " en la plataforma.\n\n"
                + "Para empezar con los cambios, cierra sesión y vuelve a iniciar sesión: "
                + "así tu cuenta cargará las nuevas opciones.\n\n"
                + "Si no esperabas este cambio, comunícate con el administrador del sistema.";
        correos.enviarPlantilla(eventoId, asignacion.correo(), ROL_ASIGNADO, ASUNTO_ROL,
                json.writeValueAsString(datos), cuerpo);
    }

    /* "Administrador de sucursal en «Xela Central»", "Administrador del sistema", ... */
    static String descripcion(String rol, String sucursalNombre) {
        String puesto = switch (rol == null ? "" : rol) {
            case "ADMIN_SISTEMA" -> "Administrador del sistema";
            case "ADMIN_SUCURSAL" -> "Administrador de sucursal";
            case "CAJERO" -> "Cajero";
            case "CHOFER" -> "Chofer";
            default -> "usuario";
        };
        boolean conSucursal = !"ADMIN_SISTEMA".equals(rol) && sucursalNombre != null && !sucursalNombre.isBlank();
        return conSucursal ? puesto + " en «" + sucursalNombre + "»" : puesto;
    }

    private static Optional<String> puestoPrincipal(List<String> roles) {
        if (roles == null) {
            return Optional.empty();
        }
        return ROLES_CON_PUESTO.stream().filter(roles::contains).findFirst();
    }
}
