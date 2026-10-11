package gt.cunoc.proyecto2.sales.tareas;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import org.junit.jupiter.api.Test;

class TareaLiberarReservasTest {

    private final VentaServicio ventas = mock(VentaServicio.class);

    private final TareaLiberarReservas tarea = new TareaLiberarReservas(ventas);

    @Test
    void ejecutarLiberaLasReservasVencidas() {
        when(ventas.liberarReservasVencidas()).thenReturn(2);

        tarea.ejecutar();

        verify(ventas).liberarReservasVencidas();
        verifyNoMoreInteractions(ventas);
    }

    @Test
    void ejecutarSinReservasVencidas() {
        when(ventas.liberarReservasVencidas()).thenReturn(0);

        tarea.ejecutar();

        verify(ventas).liberarReservasVencidas();
    }
}
