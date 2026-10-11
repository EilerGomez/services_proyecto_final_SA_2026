package gt.cunoc.proyecto2.sales.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/* Payload de ClienteRegistrado (de Identity). nombre ya viene como nombres + apellidos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteRegistrado(
        UUID usuarioId,
        String nit,
        String nombre,
        String correo) {
}
