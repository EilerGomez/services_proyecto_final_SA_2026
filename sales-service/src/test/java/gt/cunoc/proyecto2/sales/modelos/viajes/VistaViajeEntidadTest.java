package gt.cunoc.proyecto2.sales.modelos.viajes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VistaViajeEntidadTest {

    @Test
    void unViajeProgramadoEstaALaVenta() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje(UUID.randomUUID(), DatosPrueba.XELA, 40,
                EstadoViaje.PROGRAMADO);

        assertThat(viaje.estaALaVenta()).isTrue();
    }

    @Test
    void unViajeEnRutaYaNoSeVende() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje(UUID.randomUUID(), DatosPrueba.XELA, 40,
                EstadoViaje.EN_RUTA);

        assertThat(viaje.estaALaVenta()).isFalse();
    }
}
