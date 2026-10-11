package gt.cunoc.proyecto2.sales.modelos.asientos;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AsientoViajeEntidadTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T10:00:00-06:00");

    private final UUID viajeId = UUID.randomUUID();

    private AsientoViajeEntidad asiento(EstadoAsiento estado, OffsetDateTime reservadoHasta) {
        return DatosPrueba.asiento(viajeId, 5, estado, estado == EstadoAsiento.DISPONIBLE ? null : UUID.randomUUID(),
                reservadoHasta);
    }

    @Test
    void vendidoEstaOcupado() {
        assertThat(asiento(EstadoAsiento.VENDIDO, null).estaOcupado(AHORA)).isTrue();
    }

    @Test
    void disponibleNoEstaOcupado() {
        assertThat(asiento(EstadoAsiento.DISPONIBLE, null).estaOcupado(AHORA)).isFalse();
    }

    @Test
    void reservadoVigenteEstaOcupado() {
        assertThat(asiento(EstadoAsiento.RESERVADO, AHORA.plusMinutes(3)).estaOcupado(AHORA)).isTrue();
    }

    @Test
    void reservaVencidaCuentaComoLibre() {
        assertThat(asiento(EstadoAsiento.RESERVADO, AHORA.minusSeconds(1)).estaOcupado(AHORA)).isFalse();
    }

    @Test
    void reservaQueVenceJustoAhoraYaEstaLibre() {
        assertThat(asiento(EstadoAsiento.RESERVADO, AHORA).estaOcupado(AHORA)).isFalse();
    }

    @Test
    void reservadoSinFechaLimiteCuentaComoLibre() {
        assertThat(asiento(EstadoAsiento.RESERVADO, null).estaOcupado(AHORA)).isFalse();
    }

    @Test
    void laReservaSeComparaPorInstanteSinImportarLaZona() {
        // 16:05 UTC = 10:05 en Guatemala: aún vigente
        OffsetDateTime hastaEnUtc = OffsetDateTime.parse("2026-11-01T16:05:00Z");

        assertThat(asiento(EstadoAsiento.RESERVADO, hastaEnUtc).estaOcupado(AHORA)).isTrue();
    }

    @Test
    void gettersDelAsiento() {
        UUID ventaId = UUID.randomUUID();
        AsientoViajeEntidad asiento = DatosPrueba.asiento(viajeId, 12, EstadoAsiento.VENDIDO, ventaId, null);

        assertThat(asiento.getViajeId()).isEqualTo(viajeId);
        assertThat(asiento.getNumeroAsiento()).isEqualTo(12);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.VENDIDO);
        assertThat(asiento.getVentaId()).isEqualTo(ventaId);
        assertThat(asiento.getReservadoHasta()).isNull();
        assertThat(asiento.getVersion()).isZero();
    }
}
