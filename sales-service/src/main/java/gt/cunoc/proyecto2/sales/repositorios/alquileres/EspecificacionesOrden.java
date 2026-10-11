package gt.cunoc.proyecto2.sales.repositorios.alquileres;

import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import org.springframework.data.jpa.domain.Specification;

public final class EspecificacionesOrden {

    private EspecificacionesOrden() {
    }

    public static Specification<OrdenAlquilerEntidad> filtrar(FiltroOrdenes filtro) {
        Specification<OrdenAlquilerEntidad> condicion = (raiz, consulta, cb) -> cb.conjunction();
        if (filtro.sucursalId() != null) {
            condicion = condicion.and((raiz, consulta, cb) -> cb.equal(raiz.get("sucursalId"), filtro.sucursalId()));
        }
        if (filtro.estado() != null) {
            condicion = condicion.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), filtro.estado()));
        }
        return condicion;
    }
}