package gt.cunoc.proyecto2.sales.repositorios.ventas;

import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class EspecificacionesVenta {

    private static final String FECHA_PAGO = "fechaPago";

    private EspecificacionesVenta() {
    }

    public static Specification<VentaEntidad> filtrar(FiltroVentas filtro) {
        Specification<VentaEntidad> condicion = (raiz, consulta, cb) -> cb.conjunction();
        condicion = igual(condicion, "viajeId", filtro.viajeId());
        condicion = igual(condicion, "sucursalId", filtro.sucursalId());
        condicion = igual(condicion, "estado", filtro.estado());
        condicion = igual(condicion, "canal", filtro.canal());
        if (filtro.desde() != null) {
            LocalDate desde = filtro.desde();
            condicion = condicion.and((raiz, consulta, cb) ->
                    cb.greaterThanOrEqualTo(raiz.<LocalDate>get(FECHA_PAGO), desde));
        }
        if (filtro.hasta() != null) {
            LocalDate hasta = filtro.hasta();
            condicion = condicion.and((raiz, consulta, cb) ->
                    cb.lessThanOrEqualTo(raiz.<LocalDate>get(FECHA_PAGO), hasta));
        }
        return condicion;
    }

    private static Specification<VentaEntidad> igual(Specification<VentaEntidad> actual, String campo, Object valor) {
        if (valor == null) {
            return actual;
        }
        return actual.and((raiz, consulta, cb) -> cb.equal(raiz.get(campo), valor));
    }
}