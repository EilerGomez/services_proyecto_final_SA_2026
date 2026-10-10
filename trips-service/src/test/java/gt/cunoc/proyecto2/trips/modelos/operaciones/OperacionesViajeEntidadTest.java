package gt.cunoc.proyecto2.trips.modelos.operaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** SalidaViajeEntidad y LlegadaViajeEntidad. */
class OperacionesViajeEntidadTest {

    // ----- Salida -----

    @Test
    void laSalidaCopiaBusYChoferDelViaje() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        UUID registradoPor = UUID.randomUUID();

        SalidaViajeEntidad salida = SalidaViajeEntidad.registrar(viaje, DatosPrueba.SALIDA,
                new BigDecimal("1000.00"), registradoPor);

        assertThat(salida.getViajeId()).isEqualTo(viaje.getId());
        assertThat(salida.getId()).isEqualTo(viaje.getId());
        assertThat(salida.getBusId()).isEqualTo(viaje.getBusId());
        assertThat(salida.getChoferId()).isEqualTo(viaje.getChoferId());
        assertThat(salida.getFechaHoraSalida()).isEqualTo(DatosPrueba.SALIDA);
        assertThat(salida.getKmInicial()).isEqualByComparingTo("1000.00");
        assertThat(salida.getRegistradoPor()).isEqualTo(registradoPor);
    }

    @Test
    void laSalidaEsNuevaHastaQueSeGuarda() {
        SalidaViajeEntidad salida = DatosPrueba.salida(DatosPrueba.viajeRegular());

        assertThat(salida.isNew()).isTrue();

        ReflectionTestUtils.invokeMethod(salida, "marcarGuardada");

        assertThat(salida.isNew()).isFalse();
    }

    // ----- Llegada -----

    @Test
    void laLlegadaCalculaKmDepreciacionYSalario() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
        UUID registradoPor = UUID.randomUUID();

        LlegadaViajeEntidad llegada = LlegadaViajeEntidad.registrar(viaje, salida, DatosPrueba.LLEGADA,
                new BigDecimal("1207.50"), new BigDecimal("950.00"), new BigDecimal("1.2500"), registradoPor);

        assertThat(llegada.getViajeId()).isEqualTo(viaje.getId());
        assertThat(llegada.getId()).isEqualTo(viaje.getId());
        assertThat(llegada.getFechaHoraLlegada()).isEqualTo(DatosPrueba.LLEGADA);
        assertThat(llegada.getKmFinal()).isEqualByComparingTo("1207.50");
        assertThat(llegada.getKmRecorridos()).isEqualByComparingTo("207.50");
        assertThat(llegada.getCostoCombustible()).isEqualByComparingTo("950.00");
        assertThat(llegada.getTarifaDepreciacion()).isEqualByComparingTo("1.2500");
        // 207.50 × 1.25 = 259.375 → 259.38
        assertThat(llegada.getCostoDepreciacion()).isEqualByComparingTo("259.38");
        assertThat(llegada.getCostoDepreciacion().scale()).isEqualTo(2);
        assertThat(llegada.getSalarioChofer()).isEqualByComparingTo("350.00");
        assertThat(llegada.getRegistradoPor()).isEqualTo(registradoPor);
    }

    @Test
    void enViajePrivadoElSalarioLlevaElRecargo() {
        ViajeEntidad viaje = DatosPrueba.viajePrivado(DatosPrueba.alquilerPagado(), DatosPrueba.asignacion());
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);

        LlegadaViajeEntidad llegada = DatosPrueba.llegada(viaje, salida);

        assertThat(llegada.getSalarioChofer()).isEqualByComparingTo("402.50");
    }

    @Test
    void sinRecorrerKilometrosLaDepreciacionEsCero() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);

        LlegadaViajeEntidad llegada = LlegadaViajeEntidad.registrar(viaje, salida, DatosPrueba.LLEGADA,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1.2500"), UUID.randomUUID());

        assertThat(llegada.getKmRecorridos()).isEqualByComparingTo("0");
        assertThat(llegada.getCostoDepreciacion()).isEqualByComparingTo("0.00");
    }

    @Test
    void kmFinalMenorQueElInicialEsKmInvalido() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);

        assertThatThrownBy(() -> LlegadaViajeEntidad.registrar(viaje, salida, DatosPrueba.LLEGADA,
                new BigDecimal("999.99"), new BigDecimal("950.00"), new BigDecimal("1.2500"), UUID.randomUUID()))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorOperacionViaje.KM_INVALIDO.codigo());
                    assertThat(e.getMessage())
                            .isEqualTo("El kilometraje final (999.99) es menor que el inicial (1000.00)");
                });
    }

    @Test
    void llegadaIgualOAnteriorALaSalidaEsInvalida() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);   // 06:05

        VerificarError.lanza(() -> LlegadaViajeEntidad.registrar(viaje, salida, salida.getFechaHoraSalida(),
                new BigDecimal("1207.50"), new BigDecimal("950.00"), new BigDecimal("1.2500"), UUID.randomUUID()),
                ErrorOperacionViaje.LLEGADA_ANTES_DE_SALIDA);
        assertThatThrownBy(() -> LlegadaViajeEntidad.registrar(viaje, salida, DatosPrueba.SALIDA,
                new BigDecimal("1207.50"), new BigDecimal("950.00"), new BigDecimal("1.2500"), UUID.randomUUID()))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorOperacionViaje.LLEGADA_ANTES_DE_SALIDA.codigo());
                    assertThat(e.getMessage()).contains("2026-10-25T06:05-06:00");
                });
    }

    @Test
    void laLlegadaEsNuevaHastaQueSeGuarda() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        LlegadaViajeEntidad llegada = DatosPrueba.llegada(viaje, DatosPrueba.salida(viaje));

        assertThat(llegada.isNew()).isTrue();

        ReflectionTestUtils.invokeMethod(llegada, "marcarGuardada");

        assertThat(llegada.isNew()).isFalse();
    }
}
