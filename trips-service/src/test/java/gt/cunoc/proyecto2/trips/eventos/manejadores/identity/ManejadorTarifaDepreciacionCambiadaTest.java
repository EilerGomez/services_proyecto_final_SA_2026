package gt.cunoc.proyecto2.trips.eventos.manejadores.identity;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManejadorTarifaDepreciacionCambiadaTest {

    private final ProyeccionServicio proyecciones = mock(ProyeccionServicio.class);
    private final ManejadorTarifaDepreciacionCambiada manejador =
            new ManejadorTarifaDepreciacionCambiada(proyecciones);

    @Test
    void nombreYPayload() {
        assertThat(manejador.tipoEvento()).isEqualTo("TarifaDepreciacionCambiada");
        assertThat(manejador.tipoPayload()).isEqualTo(TarifaDepreciacionCambiada.class);
    }

    @Test
    void guardaLaTarifaNueva() {
        TarifaDepreciacionCambiada tarifa = new TarifaDepreciacionCambiada(UUID.randomUUID(),
                new BigDecimal("1.2500"), FECHA);

        manejador.manejar(new EventoRecibido<>(UUID.randomUUID(), "TarifaDepreciacionCambiada", 1,
                tarifa.tarifaId(), 0L, Instant.now(), FECHA, UUID.randomUUID(), tarifa));

        verify(proyecciones).guardarTarifa(tarifa);
        verifyNoMoreInteractions(proyecciones);
    }
}
