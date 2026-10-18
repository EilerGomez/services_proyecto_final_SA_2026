package gt.cunoc.proyecto2.wallet.pagos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pasarela simulada: aprueba todos los cobros al instante. Es la que se usa en desarrollo y en la
 * calificación ({@code app.pagos.proveedor=simulado}, valor por defecto).
 */
@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "simulado", matchIfMissing = true)
public class ProveedorSimulado implements ProveedorPagos {

    public static final String CODIGO = "SIMULADO";

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        return ResultadoCobro.aprobado("SIM-" + solicitud.idempotencia());
    }
}
