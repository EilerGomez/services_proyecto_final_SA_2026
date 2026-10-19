package gt.cunoc.proyecto2.notification.clientes;

import gt.cunoc.proyecto2.notification.configuracion.ConfiguracionClientes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/*
 * Llamadas de Notification a Identity (R4):
 *   - administradores de una sucursal (para avisarles un pago de alquiler),
 *   - contacto de un usuario (para el recordatorio de vencimiento de licencia).
 */
@Component
public class ClienteIdentity {

    private static final String RUTA_ADMINISTRADORES = "/identity/interno/sucursales/{id}/administradores";
    private static final String RUTA_CONTACTO = "/identity/interno/usuarios/{id}/contacto";

    private final RestClient cliente;

    public ClienteIdentity(@Qualifier(ConfiguracionClientes.CLIENTE_IDENTITY) RestClient cliente) {
        this.cliente = cliente;
    }

    /** Administradores activos de la sucursal; lista vacía si la respuesta viene sin cuerpo. */
    public List<AdminContacto> administradores(UUID sucursalId) {
        List<AdminContacto> administradores = cliente.get()
                .uri(RUTA_ADMINISTRADORES, sucursalId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AdminContacto>>() { });
        return administradores == null ? List.of() : administradores;
    }

    /** Contacto del usuario; vacío si Identity no lo tiene (la cuenta pudo eliminarse). */
    public Optional<ContactoUsuario> contacto(UUID usuarioId) {
        return Optional.ofNullable(cliente.get()
                .uri(RUTA_CONTACTO, usuarioId)
                .retrieve()
                .body(ContactoUsuario.class));
    }
}
