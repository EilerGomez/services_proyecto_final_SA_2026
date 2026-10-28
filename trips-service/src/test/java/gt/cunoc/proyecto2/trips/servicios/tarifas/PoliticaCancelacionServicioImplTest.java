package gt.cunoc.proyecto2.trips.servicios.tarifas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.alquileres.PoliticaCancelacionRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaReembolsoEntidad;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaReembolsoRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class PoliticaCancelacionServicioImplTest {

    /** DatosPrueba.alquiler() sale el 2026-11-05 a las 07:00 de Guatemala. */
    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T07:00:00-06:00");
    private static final OffsetDateTime TRES_DIAS_ANTES = SALIDA.minusHours(72);
    private static final OffsetDateTime DIEZ_HORAS_ANTES = SALIDA.minusHours(10);
    private static final OffsetDateTime YA_SALIO = SALIDA.plusHours(3);

    private final VistaTarifaReembolsoRepositorio repositorio = mock(VistaTarifaReembolsoRepositorio.class);

    private PoliticaCancelacionServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new PoliticaCancelacionServicioImpl(repositorio);
    }

    private static VistaTarifaReembolsoEntidad tarifa(String estado, int horas, boolean permite,
                                                      String porcentaje) {
        VistaTarifaReembolsoEntidad tarifa = BeanUtils.instantiateClass(VistaTarifaReembolsoEntidad.class);
        ReflectionTestUtils.setField(tarifa, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(tarifa, "estadoAlquiler", estado);
        ReflectionTestUtils.setField(tarifa, "horasMinimasAntes", horas);
        ReflectionTestUtils.setField(tarifa, "permiteCancelacion", permite);
        ReflectionTestUtils.setField(tarifa, "porcentajeReembolso", new BigDecimal(porcentaje));
        ReflectionTestUtils.setField(tarifa, "justificacion", "Tramo de " + horas + " horas");
        ReflectionTestUtils.setField(tarifa, "activa", true);
        return tarifa;
    }

    private void laCopiaTiene(VistaTarifaReembolsoEntidad... tarifas) {
        when(repositorio.findByActivaTrueAndEstadoAlquilerIn(any())).thenReturn(List.of(tarifas));
    }

    /** Lleva el alquiler hasta ASIGNADO con precio confirmado, que es cuando ya hay dinero en juego. */
    private static AlquilerPrivadoEntidad asignado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());
        alquiler.marcarPagado();
        alquiler.asignar();
        return alquiler;
    }

    @Test
    void conTresDiasDeAnticipacionGanaElTramoAlto() {
        laCopiaTiene(tarifa("ASIGNADO", 24, true, "80.00"),
                tarifa("ASIGNADO", 1, true, "60.00"),
                tarifa("ASIGNADO", 0, false, "0.00"));

        PoliticaCancelacionRespuesta politica = servicio.para(asignado(), TRES_DIAS_ANTES);

        assertThat(politica.permiteCancelacion()).isTrue();
        assertThat(politica.porcentajeReembolso()).isEqualByComparingTo("80.00");
        assertThat(politica.horasAntesDeSalida()).isEqualTo(72);
        assertThat(politica.montoEstimadoReembolso()).isEqualByComparingTo("2080.00");
    }

    @Test
    void conDiezHorasCaeAlTramoDelSesentaPorCiento() {
        laCopiaTiene(tarifa("ASIGNADO", 24, true, "80.00"),
                tarifa("ASIGNADO", 1, true, "60.00"),
                tarifa("ASIGNADO", 0, false, "0.00"));

        PoliticaCancelacionRespuesta politica = servicio.para(asignado(), DIEZ_HORAS_ANTES);

        assertThat(politica.porcentajeReembolso()).isEqualByComparingTo("60.00");
        assertThat(politica.montoEstimadoReembolso()).isEqualByComparingTo("1560.00");
    }

    @Test
    void siElViajeYaSalioNoSePermiteCancelar() {
        laCopiaTiene(tarifa("ASIGNADO", 24, true, "80.00"),
                tarifa("ASIGNADO", 0, false, "0.00"));

        PoliticaCancelacionRespuesta politica = servicio.para(asignado(), YA_SALIO);

        assertThat(politica.permiteCancelacion()).isFalse();
        assertThat(politica.horasAntesDeSalida()).isNegative();
    }

    /** Antes de pagar no hay monto que mostrar, aunque la tarifa traiga un porcentaje. */
    @Test
    void unAlquilerSolicitadoNoTraeMontoEstimado() {
        laCopiaTiene(tarifa("SOLICITADO", 0, true, "0.00"));
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();

        PoliticaCancelacionRespuesta politica = servicio.para(alquiler, TRES_DIAS_ANTES);

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.SOLICITADO);
        assertThat(politica.permiteCancelacion()).isTrue();
        assertThat(politica.montoEstimadoReembolso()).isNull();
    }

    @Test
    void elEstadoEspecificoManadaSobreElComodin() {
        laCopiaTiene(tarifa(VistaTarifaReembolsoEntidad.CUALQUIERA, 0, true, "50.00"),
                tarifa("ASIGNADO", 0, true, "80.00"));

        assertThat(servicio.para(asignado(), DIEZ_HORAS_ANTES).porcentajeReembolso())
                .isEqualByComparingTo("80.00");
    }

    /** Sin política copiada todavía, cancelar no se bloquea: es mejor permitir que trabar la sucursal. */
    @Test
    void sinTarifasEnLaCopiaSePermiteCancelar() {
        laCopiaTiene();

        PoliticaCancelacionRespuesta politica = servicio.para(asignado(), DIEZ_HORAS_ANTES);

        assertThat(politica.tarifaId()).isNull();
        assertThat(politica.permiteCancelacion()).isTrue();
        assertThat(politica.porcentajeReembolso()).isEqualByComparingTo("100.00");
        assertThat(politica.montoEstimadoReembolso()).isEqualByComparingTo("2600.00");
    }

    @Test
    void unaTarifaDeOtroEstadoNoCubre() {
        VistaTarifaReembolsoEntidad soloPagado = tarifa("PAGADO", 0, true, "95.00");

        assertThat(soloPagado.cubre(EstadoAlquiler.ASIGNADO, 100)).isFalse();
        assertThat(soloPagado.cubre(EstadoAlquiler.PAGADO, 100)).isTrue();
        assertThat(soloPagado.esComodin()).isFalse();
    }

    @Test
    void unaTarifaConTramoMayorAlTiempoRestanteNoCubre() {
        VistaTarifaReembolsoEntidad tramoAlto = tarifa("ASIGNADO", 48, true, "100.00");

        assertThat(tramoAlto.cubre(EstadoAlquiler.ASIGNADO, 10)).isFalse();
        assertThat(tramoAlto.cubre(EstadoAlquiler.ASIGNADO, 48)).isTrue();
    }

    @Test
    void lasHorasSeCuentanCompletasHaciaLaSalida() {
        assertThat(PoliticaCancelacionServicio.horasAntesDeSalida(SALIDA.minusMinutes(90), SALIDA))
                .isEqualTo(1);
        assertThat(PoliticaCancelacionServicio.horasAntesDeSalida(SALIDA.minusMinutes(30), SALIDA))
                .isZero();
    }
}
