package gt.cunoc.proyecto2.sales.dtos.asientos;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.asientos.EstadoAsiento;
import gt.cunoc.proyecto2.sales.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DtosAsientoTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T10:00:00-06:00");

    private final UUID viajeId = UUID.randomUUID();

    private AsientoViajeEntidad asiento(int numero, EstadoAsiento estado, OffsetDateTime reservadoHasta) {
        UUID ventaId = estado == EstadoAsiento.DISPONIBLE ? null : UUID.randomUUID();
        return DatosPrueba.asiento(viajeId, numero, estado, ventaId, reservadoHasta);
    }

    // ----- AsientoRespuesta -----

    @Test
    void asientoDisponible() {
        AsientoRespuesta respuesta = new AsientoRespuesta(asiento(1, EstadoAsiento.DISPONIBLE, null), AHORA);

        assertThat(respuesta.numero()).isEqualTo(1);
        assertThat(respuesta.estado()).isEqualTo(EstadoMapaAsiento.DISPONIBLE);
        assertThat(respuesta.disponible()).isTrue();
    }

    @Test
    void vendidoYReservadoVigenteSeMuestranOcupados() {
        AsientoRespuesta vendido = new AsientoRespuesta(asiento(2, EstadoAsiento.VENDIDO, null), AHORA);
        AsientoRespuesta reservado = new AsientoRespuesta(
                asiento(3, EstadoAsiento.RESERVADO, AHORA.plusMinutes(4)), AHORA);

        assertThat(vendido.estado()).isEqualTo(EstadoMapaAsiento.OCUPADO);
        assertThat(vendido.disponible()).isFalse();
        assertThat(reservado.estado()).isEqualTo(EstadoMapaAsiento.OCUPADO);
        assertThat(reservado.disponible()).isFalse();
    }

    @Test
    void reservaVencidaSeMuestraDisponible() {
        AsientoRespuesta respuesta = new AsientoRespuesta(
                asiento(4, EstadoAsiento.RESERVADO, AHORA.minusMinutes(1)), AHORA);

        assertThat(respuesta.numero()).isEqualTo(4);
        assertThat(respuesta.estado()).isEqualTo(EstadoMapaAsiento.DISPONIBLE);
        assertThat(respuesta.disponible()).isTrue();
    }

    // ----- MapaAsientosRespuesta -----

    @Test
    void mapaConLosDatosDelViajeYLosDisponibles() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje(viajeId, DatosPrueba.XELA, 5, EstadoViaje.PROGRAMADO);
        // 14:00 UTC = 08:00 en Guatemala
        ReflectionTestUtils.setField(viaje, "fechaSalida", OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.UTC));
        List<AsientoViajeEntidad> asientos = List.of(
                asiento(1, EstadoAsiento.VENDIDO, null),
                asiento(2, EstadoAsiento.RESERVADO, AHORA.plusMinutes(2)),
                asiento(3, EstadoAsiento.RESERVADO, AHORA.minusMinutes(2)),
                asiento(4, EstadoAsiento.DISPONIBLE, null),
                asiento(5, EstadoAsiento.DISPONIBLE, null));

        MapaAsientosRespuesta mapa = MapaAsientosRespuesta.de(viaje, asientos, AHORA);

        assertThat(mapa.viajeId()).isEqualTo(viajeId);
        assertThat(mapa.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(mapa.destinoNombre()).isEqualTo("Guatemala");
        assertThat(mapa.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
        assertThat(mapa.fechaSalida().getOffset()).isEqualTo(GUATEMALA);
        assertThat(mapa.busPlaca()).isEqualTo("P-123ABC");
        assertThat(mapa.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(mapa.capacidad()).isEqualTo(5);
        assertThat(mapa.disponibles()).isEqualTo(3);
        assertThat(mapa.asientos()).extracting(AsientoRespuesta::numero).containsExactly(1, 2, 3, 4, 5);
        assertThat(mapa.asientos()).extracting(AsientoRespuesta::estado).containsExactly(
                EstadoMapaAsiento.OCUPADO, EstadoMapaAsiento.OCUPADO, EstadoMapaAsiento.DISPONIBLE,
                EstadoMapaAsiento.DISPONIBLE, EstadoMapaAsiento.DISPONIBLE);
    }

    @Test
    void mapaSinAsientos() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();

        MapaAsientosRespuesta mapa = MapaAsientosRespuesta.de(viaje, List.of(), AHORA);

        assertThat(mapa.capacidad()).isEqualTo(40);
        assertThat(mapa.disponibles()).isZero();
        assertThat(mapa.asientos()).isEmpty();
        assertThat(mapa.fechaSalida()).isEqualTo(DatosPrueba.SALIDA);
    }
}
