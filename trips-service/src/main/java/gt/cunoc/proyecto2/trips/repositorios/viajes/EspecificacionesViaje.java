package gt.cunoc.proyecto2.trips.repositorios.viajes;

import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class EspecificacionesViaje {

    private static final String SALIDA = "fechaSalidaProgramada";

    private EspecificacionesViaje() {
    }

    public static Specification<ViajeEntidad> filtrar(FiltroViajes filtro) {
        // Rango por día de salida en Guatemala; sin fechas, los límites amplios de Fechas
        OffsetDateTime desde = Fechas.inicioDelDia(filtro.desde());
        OffsetDateTime hasta = Fechas.finDelDia(filtro.hasta());
        Specification<ViajeEntidad> condicion = (raiz, consulta, cb) -> cb.and(
                cb.greaterThanOrEqualTo(raiz.<OffsetDateTime>get(SALIDA), desde),
                cb.lessThan(raiz.<OffsetDateTime>get(SALIDA), hasta));
        condicion = igual(condicion, "rutaId", filtro.rutaId());
        condicion = igual(condicion, "sucursalId", filtro.sucursalId());
        condicion = igual(condicion, "choferId", filtro.choferId());
        condicion = igual(condicion, "estado", filtro.estado());
        condicion = igual(condicion, "tipo", filtro.tipo());
        return condicion;
    }

    private static Specification<ViajeEntidad> igual(Specification<ViajeEntidad> actual, String campo, Object valor) {
        if (valor == null) {
            return actual;
        }
        return actual.and((raiz, consulta, cb) -> cb.equal(raiz.get(campo), valor));
    }
}