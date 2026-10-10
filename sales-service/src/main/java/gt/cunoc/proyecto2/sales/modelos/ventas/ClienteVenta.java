package gt.cunoc.proyecto2.sales.modelos.ventas;

import java.util.UUID;

public record ClienteVenta(UUID usuarioId, String nit, String nombre, String correo) {

    public static final String CONSUMIDOR_FINAL_NIT = "CF";
    public static final String CONSUMIDOR_FINAL_NOMBRE = "Consumidor final";

    public ClienteVenta {
        nit = nit == null || nit.isBlank() ? CONSUMIDOR_FINAL_NIT : nit;
        nombre = nombre == null || nombre.isBlank() ? CONSUMIDOR_FINAL_NOMBRE : nombre;
    }

    public boolean esConsumidorFinal() {
        return CONSUMIDOR_FINAL_NIT.equals(nit);
    }
}