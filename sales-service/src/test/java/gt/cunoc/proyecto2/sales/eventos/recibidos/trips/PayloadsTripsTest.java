package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayloadsTripsTest {

    private ViajeEvento viajeEvento(String tipo) {
        return new ViajeEvento(UUID.randomUUID(), tipo, UUID.randomUUID(), DatosPrueba.XELA, "Quetzaltenango",
                "Guatemala", UUID.randomUUID(), "P-123ABC", 40, new BigDecimal("75.00"), DatosPrueba.SALIDA);
    }

    @Test
    void elTipoRegularEsElQueSeVendePorBoleto() {
        assertThat(TiposViaje.REGULAR).isEqualTo("REGULAR");
    }

    @Test
    void viajeEventoSoloEsRegularConTipoRegular() {
        assertThat(viajeEvento("REGULAR").esRegular()).isTrue();
        assertThat(viajeEvento("PRIVADO").esRegular()).isFalse();
        assertThat(viajeEvento(null).esRegular()).isFalse();
    }

    @Test
    void viajeIniciadoSoloEsRegularConTipoRegular() {
        UUID viajeId = UUID.randomUUID();

        assertThat(new ViajeIniciado(viajeId, "REGULAR").esRegular()).isTrue();
        assertThat(new ViajeIniciado(viajeId, "PRIVADO").esRegular()).isFalse();
        assertThat(new ViajeIniciado(viajeId, null).esRegular()).isFalse();
    }

    @Test
    void viajeEliminadoSoloEsRegularConTipoRegular() {
        UUID viajeId = UUID.randomUUID();

        assertThat(new ViajeEliminado(viajeId, "REGULAR", DatosPrueba.XELA, "Bus averiado").esRegular()).isTrue();
        assertThat(new ViajeEliminado(viajeId, "PRIVADO", DatosPrueba.XELA, "Bus averiado").esRegular()).isFalse();
        assertThat(new ViajeEliminado(viajeId, null, null, null).esRegular()).isFalse();
    }

    @Test
    void elTipoDistingueMayusculas() {
        assertThat(viajeEvento("regular").esRegular()).isFalse();
    }
}
