package gt.cunoc.proyecto2.trips.servicios.tarifas;

import gt.cunoc.proyecto2.trips.dtos.alquileres.PoliticaCancelacionRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaReembolsoEntidad;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaReembolsoRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PoliticaCancelacionServicioImpl implements PoliticaCancelacionServicio {

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * Gana el tramo más alto que la cancelación alcanza; si dos tramos empatan, el del estado
     * específico antes que el comodín (una regla escrita para ASIGNADO manda sobre una de CUALQUIERA).
     */
    private static final Comparator<VistaTarifaReembolsoEntidad> MAS_ESPECIFICA =
            Comparator.comparingInt(VistaTarifaReembolsoEntidad::getHorasMinimasAntes).reversed()
                    .thenComparing(Comparator.comparing(
                            (VistaTarifaReembolsoEntidad tarifa) -> tarifa.esComodin()));

    private final VistaTarifaReembolsoRepositorio repositorio;

    public PoliticaCancelacionServicioImpl(VistaTarifaReembolsoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticaCancelacionRespuesta para(AlquilerPrivadoEntidad alquiler, OffsetDateTime cancelacion) {
        EstadoAlquiler estado = alquiler.getEstado();
        long horas = PoliticaCancelacionServicio.horasAntesDeSalida(cancelacion, alquiler.getFechaSalida());
        // Si el viaje ya debió salir, aplica el tramo de 0 horas (el último, el que no permite cancelar)
        long paraBuscar = Math.max(horas, 0);
        return repositorio.findByActivaTrueAndEstadoAlquilerIn(
                        List.of(estado.name(), VistaTarifaReembolsoEntidad.CUALQUIERA)).stream()
                .filter(tarifa -> tarifa.cubre(estado, paraBuscar))
                .min(MAS_ESPECIFICA)
                .map(tarifa -> PoliticaCancelacionRespuesta.de(tarifa, horas,
                        estimado(alquiler, tarifa.getPorcentajeReembolso())))
                .orElseGet(() -> PoliticaCancelacionRespuesta.porDefecto(horas, estimado(alquiler, CIEN)));
    }

    /**
     * Lo que se le devolvería. Null mientras el alquiler no se haya pagado: antes de PAGADO no hay
     * dinero en juego y mostrar un monto solo confundiría.
     */
    private static BigDecimal estimado(AlquilerPrivadoEntidad alquiler, BigDecimal porcentaje) {
        BigDecimal pagado = alquiler.getPrecioConfirmado();
        if (pagado == null || !alquiler.yaSePago()) {
            return null;
        }
        return pagado.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }
}
