package gt.cunoc.proyecto2.sales.controladores.asientos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.dtos.asientos.MapaAsientosRespuesta;
import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.asientos.EstadoAsiento;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.servicios.asientos.AsientoServicio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AsientoControladorTest {

    private final AsientoServicio servicio = mock(AsientoServicio.class);
    private final AsientoControlador controlador = new AsientoControlador(servicio);

    // ----- S1 -----

    @Test
    void mapaRespondeOkConLoQueArmaElServicio() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        UUID viajeId = viaje.getViajeId();
        List<AsientoViajeEntidad> asientos = List.of(
                DatosPrueba.asiento(viajeId, 1, EstadoAsiento.DISPONIBLE, null, null),
                DatosPrueba.asiento(viajeId, 2, EstadoAsiento.VENDIDO, UUID.randomUUID(), null));
        MapaAsientosRespuesta mapa = MapaAsientosRespuesta.de(viaje, asientos, OffsetDateTime.now());
        when(servicio.mapa(viajeId)).thenReturn(mapa);

        ResponseEntity<MapaAsientosRespuesta> respuesta = controlador.mapa(viajeId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(mapa);
        assertThat(respuesta.getBody().disponibles()).isEqualTo(1);
        verify(servicio).mapa(viajeId);
    }
}
