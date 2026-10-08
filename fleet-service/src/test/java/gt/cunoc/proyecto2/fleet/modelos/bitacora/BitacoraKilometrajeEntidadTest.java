package gt.cunoc.proyecto2.fleet.modelos.bitacora;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.bitacora.ErrorBitacora;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BitacoraKilometrajeEntidadTest {

    private final UUID busId = UUID.randomUUID();
    private final UUID viajeId = UUID.randomUUID();
    private final OffsetDateTime llegada = OffsetDateTime.parse("2026-10-20T18:30:00-06:00");

    @Test
    void registraElViajeYCalculaLosKmRecorridos() {
        BitacoraKilometrajeEntidad registro = BitacoraKilometrajeEntidad.registrar(busId, viajeId,
                new BigDecimal("1000.00"), new BigDecimal("1201.50"), llegada);

        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getBusId()).isEqualTo(busId);
        assertThat(registro.getViajeId()).isEqualTo(viajeId);
        assertThat(registro.getFechaLlegada()).isEqualTo(llegada);
        assertThat(registro.kmRecorridos()).isEqualByComparingTo("201.50");
    }

    @Test
    void elKilometrajeFinalNoPuedeSerMenor() {
        VerificarError.lanza(() -> BitacoraKilometrajeEntidad.registrar(busId, viajeId,
                new BigDecimal("1000.00"), new BigDecimal("999.99"), llegada), ErrorBitacora.KILOMETRAJE_INVALIDO);
    }
}
