package gt.cunoc.proyecto2.wallet.clientes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Respuesta de Identity R4c (GET /identity/interno/usuarios/{id}/estado), solo lo que usa la cartera.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstadoCuenta(UUID usuarioId, String estado) {

    public boolean activa() {
        return "ACTIVO".equals(estado);
    }
}
