package gt.cunoc.proyecto2.trips.modelos.rutas;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.rutas.ErrorRuta;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RutaEntidadTest {

    @Test
    void unaRutaNuevaQuedaActivaYSinVersion() {
        UUID creadoPor = UUID.randomUUID();

        RutaEntidad ruta = RutaEntidad.registrar(DatosPrueba.XELA, DatosPrueba.GUATE, new BigDecimal("205.00"),
                new BigDecimal("75.00"), creadoPor);

        assertThat(ruta.getId()).isNotNull();
        assertThat(ruta.getSucursalOrigenId()).isEqualTo(DatosPrueba.XELA);
        assertThat(ruta.getSucursalDestinoId()).isEqualTo(DatosPrueba.GUATE);
        assertThat(ruta.getDistanciaKm()).isEqualByComparingTo("205.00");
        assertThat(ruta.getPrecioBoleto()).isEqualByComparingTo("75.00");
        assertThat(ruta.isActiva()).isTrue();
        assertThat(ruta.getCreadoPor()).isEqualTo(creadoPor);
        assertThat(ruta.getVersion()).isNull();
    }

    @Test
    void origenYDestinoIgualesEsMismaSucursal() {
        VerificarError.lanza(() -> RutaEntidad.registrar(DatosPrueba.XELA, DatosPrueba.XELA,
                new BigDecimal("10.00"), new BigDecimal("5.00"), UUID.randomUUID()), ErrorRuta.MISMA_SUCURSAL);
    }

    @Test
    void actualizarCambiaDistanciaPrecioYEstado() {
        RutaEntidad ruta = DatosPrueba.ruta();

        ruta.actualizar(new BigDecimal("210.50"), new BigDecimal("80.00"), false);

        assertThat(ruta.getDistanciaKm()).isEqualByComparingTo("210.50");
        assertThat(ruta.getPrecioBoleto()).isEqualByComparingTo("80.00");
        assertThat(ruta.isActiva()).isFalse();
        assertThat(ruta.getSucursalOrigenId()).isEqualTo(DatosPrueba.XELA);
        assertThat(ruta.getSucursalDestinoId()).isEqualTo(DatosPrueba.GUATE);
    }

    @Test
    void seReactivaSoloAlPasarDeInactivaAActiva() {
        RutaEntidad activa = DatosPrueba.ruta();
        assertThat(activa.seReactiva(true)).isFalse();
        assertThat(activa.seReactiva(false)).isFalse();

        RutaEntidad inactiva = DatosPrueba.ruta();
        inactiva.actualizar(inactiva.getDistanciaKm(), inactiva.getPrecioBoleto(), false);
        assertThat(inactiva.seReactiva(true)).isTrue();
        assertThat(inactiva.seReactiva(false)).isFalse();
    }
}
