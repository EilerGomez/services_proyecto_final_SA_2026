package gt.cunoc.proyecto2.trips.repositorios.alquileres;

import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.domain.Specification;

/** Búsqueda de alquileres (T17) con filtros opcionales; mismo enfoque que EspecificacionesViaje. */
public final class EspecificacionesAlquiler {

    private static final String SALIDA = "fechaSalida";

    private EspecificacionesAlquiler() {
    }

    public static Specification<AlquilerPrivadoEntidad> filtrar(FiltroAlquileres filtro) {
        // Rango por día de salida en Guatemala; sin fechas, los límites amplios de Fechas
        OffsetDateTime desde = Fechas.inicioDelDia(filtro.desde());
        OffsetDateTime hasta = Fechas.finDelDia(filtro.hasta());
        Specification<AlquilerPrivadoEntidad> condicion = (raiz, consulta, cb) -> cb.and(
                cb.greaterThanOrEqualTo(raiz.<OffsetDateTime>get(SALIDA), desde),
                cb.lessThan(raiz.<OffsetDateTime>get(SALIDA), hasta));
        if (filtro.sucursalId() != null) {
            condicion = condicion.and((raiz, consulta, cb) -> cb.equal(raiz.get("sucursalId"), filtro.sucursalId()));
        }
        if (filtro.estado() != null) {
            condicion = condicion.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), filtro.estado()));
        }
        return condicion;
    }
}