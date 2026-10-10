package gt.cunoc.proyecto2.trips.dtos.operaciones;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosOperacionTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);

    @Test
    void registrarSalidaCreaLaEntidad() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        UUID registradoPor = UUID.randomUUID();
        RegistrarSalidaSolicitud solicitud = new RegistrarSalidaSolicitud(DatosPrueba.SALIDA,
                new BigDecimal("1000.00"));

        SalidaViajeEntidad salida = solicitud.crearEntidad(viaje, registradoPor);

        assertThat(salida.getViajeId()).isEqualTo(viaje.getId());
        assertThat(salida.getBusId()).isEqualTo(viaje.getBusId());
        assertThat(salida.getChoferId()).isEqualTo(viaje.getChoferId());
        assertThat(salida.getFechaHoraSalida()).isEqualTo(DatosPrueba.SALIDA);
        assertThat(salida.getKmInicial()).isEqualByComparingTo("1000.00");
        assertThat(salida.getRegistradoPor()).isEqualTo(registradoPor);
        assertThat(salida.isNew()).isTrue();
    }

    @Test
    void registrarLlegadaCreaLaEntidadConCostos() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
        UUID registradoPor = UUID.randomUUID();
        RegistrarLlegadaSolicitud solicitud = new RegistrarLlegadaSolicitud(DatosPrueba.LLEGADA,
                new BigDecimal("1207.50"), new BigDecimal("950.00"));

        LlegadaViajeEntidad llegada = solicitud.crearEntidad(viaje, salida, new BigDecimal("1.2500"), registradoPor);

        assertThat(llegada.getViajeId()).isEqualTo(viaje.getId());
        assertThat(llegada.getFechaHoraLlegada()).isEqualTo(DatosPrueba.LLEGADA);
        assertThat(llegada.getKmFinal()).isEqualByComparingTo("1207.50");
        assertThat(llegada.getKmRecorridos()).isEqualByComparingTo("207.50");
        assertThat(llegada.getCostoCombustible()).isEqualByComparingTo("950.00");
        assertThat(llegada.getCostoDepreciacion()).isEqualByComparingTo("259.38");
        assertThat(llegada.getSalarioChofer()).isEqualByComparingTo("350.00");
        assertThat(llegada.getRegistradoPor()).isEqualTo(registradoPor);
    }

    @Test
    void registrarLlegadaConKmMenorEsKmInvalido() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
        RegistrarLlegadaSolicitud solicitud = new RegistrarLlegadaSolicitud(DatosPrueba.LLEGADA,
                new BigDecimal("500.00"), new BigDecimal("950.00"));

        VerificarError.lanza(() -> solicitud.crearEntidad(viaje, salida, new BigDecimal("1.2500"), UUID.randomUUID()),
                ErrorOperacionViaje.KM_INVALIDO);
    }

    @Test
    void salidaRespuestaConHoraDeGuatemala() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        UUID registradoPor = UUID.randomUUID();
        // 12:05 UTC = 06:05 en Guatemala
        SalidaViajeEntidad salida = SalidaViajeEntidad.registrar(viaje,
                OffsetDateTime.of(2026, 10, 25, 12, 5, 0, 0, ZoneOffset.UTC), new BigDecimal("1000.00"),
                registradoPor);

        SalidaRespuesta respuesta = new SalidaRespuesta(salida);

        assertThat(respuesta.fechaHoraSalida()).isEqualTo(OffsetDateTime.of(2026, 10, 25, 6, 5, 0, 0, GUATEMALA));
        assertThat(respuesta.kmInicial()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.registradoPor()).isEqualTo(registradoPor);
    }

    @Test
    void llegadaRespuestaConCostosYHoraDeGuatemala() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
        // 16:40 UTC = 10:40 en Guatemala
        LlegadaViajeEntidad llegada = LlegadaViajeEntidad.registrar(viaje, salida,
                OffsetDateTime.of(2026, 10, 25, 16, 40, 0, 0, ZoneOffset.UTC), new BigDecimal("1207.50"),
                new BigDecimal("950.00"), new BigDecimal("1.2500"), UUID.randomUUID());

        LlegadaRespuesta respuesta = new LlegadaRespuesta(llegada);

        assertThat(respuesta.fechaHoraLlegada()).isEqualTo(OffsetDateTime.of(2026, 10, 25, 10, 40, 0, 0, GUATEMALA));
        assertThat(respuesta.kmFinal()).isEqualByComparingTo("1207.50");
        assertThat(respuesta.kmRecorridos()).isEqualByComparingTo("207.50");
        assertThat(respuesta.costoCombustible()).isEqualByComparingTo("950.00");
        assertThat(respuesta.tarifaDepreciacion()).isEqualByComparingTo("1.2500");
        assertThat(respuesta.costoDepreciacion()).isEqualByComparingTo("259.38");
        assertThat(respuesta.salarioChofer()).isEqualByComparingTo("350.00");
    }
}
