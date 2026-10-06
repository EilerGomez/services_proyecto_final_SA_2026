package gt.cunoc.proyecto2.identity.evento.payload;


import java.util.UUID;


/**
 * Payload de ClienteRegistrado.
 */
public record ClienteRegistradoPayload(


        UUID usuarioId,


        String nit,


        String nombre,


        String correo

) {
}
