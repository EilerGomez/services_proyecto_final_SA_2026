package gt.cunoc.proyecto2.sales.modelos.tarifas;

/**
 * A qué estado del alquiler aplica una tarifa de reembolso. Los cuatro primeros son los estados
 * cancelables de Trips (columna alquileres_privados.estado); CUALQUIERA es el comodín que cubre
 * los que no tienen una fila propia.
 *
 * <p>Sales no tiene la máquina de estados del alquiler (vive en Trips): acá solo interesa el nombre
 * para resolver la tarifa, así que es un enum aparte y no una copia de EstadoAlquiler.</p>
 */
public enum EstadoAlquilerTarifa {
    SOLICITADO,
    COTIZADO,
    PAGADO,
    ASIGNADO,
    /** Comodín: aplica a cualquier estado que no tenga una tarifa propia para ese tramo de horas. */
    CUALQUIERA;

    /** El estado que viene en un evento, o CUALQUIERA si no se reconoce (nunca falla por un nombre nuevo). */
    public static EstadoAlquilerTarifa deNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return CUALQUIERA;
        }
        for (EstadoAlquilerTarifa estado : values()) {
            if (estado.name().equalsIgnoreCase(nombre.trim())) {
                return estado;
            }
        }
        return CUALQUIERA;
    }

    public boolean esComodin() {
        return this == CUALQUIERA;
    }
}
