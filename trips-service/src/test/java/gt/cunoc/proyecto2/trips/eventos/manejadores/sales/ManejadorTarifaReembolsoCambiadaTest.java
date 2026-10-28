package gt.cunoc.proyecto2.trips.eventos.manejadores.sales;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.recibidos.sales.TarifaReembolsoCambiada;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManejadorTarifaReembolsoCambiadaTest {

    private final ProyeccionServicio proyecciones = mock(ProyeccionServicio.class);
    private final ManejadorTarifaReembolsoCambiada manejador =
            new ManejadorTarifaReembolsoCambiada(proyecciones);

    @Test
    void nombreYPayload() {
        assertThat(manejador.tipoEvento()).isEqualTo("TarifaReembolsoCambiada");
        assertThat(manejador.tipoPayload()).isEqualTo(TarifaReembolsoCambiada.class);
    }

    @Test
    void copiaLaTarifaEnLaVistaLocal() {
        UUID tarifaId = UUID.randomUUID();
        TarifaReembolsoCambiada payload = new TarifaReembolsoCambiada(tarifaId, "ASIGNADO", 24, true,
                new BigDecimal("80.00"), "Con 24 horas o más", true);

        manejador.manejar(new EventoRecibido<>(UUID.randomUUID(), "TarifaReembolsoCambiada", 1, tarifaId,
                2L, Instant.now(), FECHA, UUID.randomUUID(), payload));

        verify(proyecciones).guardarTarifaReembolso(payload);
        verifyNoMoreInteractions(proyecciones);
    }
}
