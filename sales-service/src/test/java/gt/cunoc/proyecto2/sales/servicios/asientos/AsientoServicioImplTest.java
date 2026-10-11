package gt.cunoc.proyecto2.sales.servicios.asientos;

import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.dtos.asientos.AsientoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.asientos.EstadoMapaAsiento;
import gt.cunoc.proyecto2.sales.dtos.asientos.MapaAsientosRespuesta;
import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.modelos.asientos.EstadoAsiento;
import gt.cunoc.proyecto2.sales.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.servicios.viajes.VistaViajeServicio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AsientoServicioImplTest {

    private final VistaViajeServicio viajes = mock(VistaViajeServicio.class);
    private final AsientoViajeRepositorio asientos =
            mock(AsientoViajeRepositorio.class);

    private final AsientoServicioImpl servicio =
            new AsientoServicioImpl(viajes, asientos);

    @Test
    void mapaDeUnViajeQueNoEstaALaVenta() {
        UUID id = UUID.randomUUID();

        when(viajes.obtenerALaVenta(id))
                .thenThrow(ErrorViaje.VIAJE_NO_DISPONIBLE.excepcion());

        VerificarError.lanza(
                () -> servicio.mapa(id),
                ErrorViaje.VIAJE_NO_DISPONIBLE);

        verifyNoInteractions(asientos);
    }

    @Test
    void mapaMuestraOcupadosLosVendidosYLasReservasVigentes() {
        UUID id = UUID.randomUUID();
        VistaViajeEntidad viaje =
                DatosPrueba.vistaViaje(id, XELA, 5, EstadoViaje.PROGRAMADO);
        OffsetDateTime ahora = OffsetDateTime.now();

        when(viajes.obtenerALaVenta(id)).thenReturn(viaje);
        when(asientos.findByViajeIdOrderByNumeroAsientoAsc(id))
                .thenReturn(List.of(
                        DatosPrueba.asiento(
                                id, 1, EstadoAsiento.DISPONIBLE, null, null),
                        DatosPrueba.asiento(
                                id, 2, EstadoAsiento.VENDIDO,
                                UUID.randomUUID(), null),
                        DatosPrueba.asiento(
                                id, 3, EstadoAsiento.RESERVADO,
                                UUID.randomUUID(), ahora.plusMinutes(5)),
                        DatosPrueba.asiento(
                                id, 4, EstadoAsiento.RESERVADO,
                                UUID.randomUUID(), ahora.minusMinutes(1)),
                        DatosPrueba.asiento(
                                id, 5, EstadoAsiento.RESERVADO,
                                UUID.randomUUID(), null)));

        MapaAsientosRespuesta mapa = servicio.mapa(id);

        assertThat(mapa.viajeId()).isEqualTo(id);
        assertThat(mapa.capacidad()).isEqualTo(5);
        assertThat(mapa.precioBoleto()).isEqualByComparingTo("75.00");

        assertThat(mapa.asientos())
                .extracting(AsientoRespuesta::numero)
                .containsExactly(1, 2, 3, 4, 5);

        assertThat(mapa.asientos())
                .extracting(AsientoRespuesta::estado)
                .containsExactly(
                        EstadoMapaAsiento.DISPONIBLE,
                        EstadoMapaAsiento.OCUPADO,
                        EstadoMapaAsiento.OCUPADO,
                        EstadoMapaAsiento.DISPONIBLE,
                        EstadoMapaAsiento.DISPONIBLE);

        assertThat(mapa.disponibles()).isEqualTo(3L);
    }
}