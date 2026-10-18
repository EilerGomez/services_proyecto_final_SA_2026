package gt.cunoc.proyecto2.wallet.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * CuentaCreada (Identity), solo lo que usa la cartera. El correo y el resto del payload se ignoran.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CuentaCreada(

        UUID usuarioId,
        List<String> roles

) {

    public boolean esCliente() {
        return roles != null && roles.contains("CLIENTE");
    }
}
