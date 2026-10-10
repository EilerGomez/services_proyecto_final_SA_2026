package gt.cunoc.proyecto2.trips.modelos.viajes;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoViaje {
    PROGRAMADO,
    EN_RUTA,
    COMPLETADO;

    public static final Set<EstadoViaje> ACTIVOS = EnumSet.of(PROGRAMADO, EN_RUTA);
}