package gt.cunoc.proyecto2.trips.eventos.manejadores.sales;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.trips.servicios.alquileres.AlquilerServicio;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManejadorAlquilerPagadoTest {

    private final AlquilerServicio alquileres = mock(AlquilerServicio.class);
    private final ManejadorAlquilerPagado manejador = new ManejadorAlquilerPagado(alquileres);

    @Test
    void nombreYPayload() {
        assertThat(manejador.tipoEvento()).isEqualTo("AlquilerPagado");
        assertThat(manejador.tipoPayload()).isEqualTo(AlquilerPagado.class);
    }

    @Test
    void registraElPagoDelAlquilerNoDeLaOrden() {
        UUID ordenId = UUID.randomUUID();
        UUID alquilerId = UUID.randomUUID();

        manejador.manejar(new EventoRecibido<>(UUID.randomUUID(), "AlquilerPagado", 1, ordenId, 3L,
                Instant.now(), FECHA, UUID.randomUUID(), new AlquilerPagado(ordenId, alquilerId)));

        verify(alquileres).registrarPago(alquilerId);
        verifyNoMoreInteractions(alquileres);
    }
}
