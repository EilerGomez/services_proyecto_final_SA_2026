package gt.cunoc.proyecto2.trips.dtos.rutas;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.rutas.ErrorRuta;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosRutaTest {

    @Test
    void nuevaRutaCreaLaEntidadActiva() {
        UUID creadoPor = UUID.randomUUID();
        NuevaRutaSolicitud solicitud = new NuevaRutaSolicitud(DatosPrueba.XELA, DatosPrueba.GUATE,
                new BigDecimal("205.00"), new BigDecimal("75.00"));

        RutaEntidad ruta = solicitud.crearEntidad(creadoPor);

        assertThat(ruta.getSucursalOrigenId()).isEqualTo(DatosPrueba.XELA);
        assertThat(ruta.getSucursalDestinoId()).isEqualTo(DatosPrueba.GUATE);
        assertThat(ruta.getDistanciaKm()).isEqualByComparingTo("205.00");
        assertThat(ruta.getPrecioBoleto()).isEqualByComparingTo("75.00");
        assertThat(ruta.isActiva()).isTrue();
        assertThat(ruta.getCreadoPor()).isEqualTo(creadoPor);
    }

    @Test
    void nuevaRutaConLaMismaSucursalEsInvalida() {
        NuevaRutaSolicitud solicitud = new NuevaRutaSolicitud(DatosPrueba.XELA, DatosPrueba.XELA,
                new BigDecimal("1.00"), new BigDecimal("1.00"));

        VerificarError.lanza(() -> solicitud.crearEntidad(UUID.randomUUID()), ErrorRuta.MISMA_SUCURSAL);
    }

    @Test
    void actualizarRutaAplicaLosCambios() {
        RutaEntidad ruta = DatosPrueba.ruta();
        ActualizarRutaSolicitud solicitud = new ActualizarRutaSolicitud(new BigDecimal("210.00"),
                new BigDecimal("80.00"), false, 0);

        solicitud.aplicarA(ruta);

        assertThat(ruta.getDistanciaKm()).isEqualByComparingTo("210.00");
        assertThat(ruta.getPrecioBoleto()).isEqualByComparingTo("80.00");
        assertThat(ruta.isActiva()).isFalse();
        assertThat(solicitud.version()).isZero();
    }

    @Test
    void respuestaConNombresDeSucursales() {
        RutaEntidad ruta = DatosPrueba.ruta();
        Map<UUID, String> nombres = Map.of(DatosPrueba.XELA, "Quetzaltenango", DatosPrueba.GUATE, "Guatemala");

        RutaRespuesta respuesta = new RutaRespuesta(ruta, nombres);

        assertThat(respuesta.rutaId()).isEqualTo(ruta.getId());
        assertThat(respuesta.sucursalOrigenId()).isEqualTo(DatosPrueba.XELA);
        assertThat(respuesta.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(respuesta.sucursalDestinoId()).isEqualTo(DatosPrueba.GUATE);
        assertThat(respuesta.destinoNombre()).isEqualTo("Guatemala");
        assertThat(respuesta.distanciaKm()).isEqualByComparingTo("205.00");
        assertThat(respuesta.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(respuesta.activa()).isTrue();
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void respuestaSinNombresLosDejaNull() {
        RutaRespuesta respuesta = new RutaRespuesta(DatosPrueba.ruta(), Map.of());

        assertThat(respuesta.origenNombre()).isNull();
        assertThat(respuesta.destinoNombre()).isNull();
    }
}
