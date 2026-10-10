package gt.cunoc.proyecto2.trips.modelos.alquileres;

import java.util.UUID;

/**
 * Datos del cliente de un alquiler. usuarioId es null si se pidió en mostrador (PRESENCIAL).
 * Sin NIT, el cliente es consumidor final ("CF").
 */
public record ClienteAlquiler(UUID usuarioId, String nit, String nombre, String correo) {

    public static final String CONSUMIDOR_FINAL = "CF";

    public ClienteAlquiler {
        nit = nit == null || nit.isBlank() ? CONSUMIDOR_FINAL : nit;
    }
}