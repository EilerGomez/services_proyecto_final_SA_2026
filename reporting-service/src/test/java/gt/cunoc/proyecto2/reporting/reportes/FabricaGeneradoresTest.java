package gt.cunoc.proyecto2.reporting.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGanancias;
import java.util.List;
import org.junit.jupiter.api.Test;

class FabricaGeneradoresTest {

    @Test
    void obtieneElGeneradorRegistrado() {
        GeneradorGanancias ganancias = mock(GeneradorGanancias.class);
        when(ganancias.tipo()).thenReturn(TipoReporte.GANANCIAS);
        FabricaGeneradores fabrica = new FabricaGeneradores(List.of(ganancias));

        assertThat(fabrica.obtener(TipoReporte.GANANCIAS)).isSameAs(ganancias);
    }

    @Test
    void tipoSinGeneradorLanza404() {
        FabricaGeneradores fabrica = new FabricaGeneradores(List.of());

        assertThatThrownBy(() -> fabrica.obtener(TipoReporte.GASTOS))
                .hasMessageContaining("GASTOS");
    }
}
