package gt.cunoc.proyecto2.maintenance.servicios.buses;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** El descarte de versiones viejas lo hace el SQL (WHERE version <=); aquí se verifica qué se le pasa. */
@ExtendWith(MockitoExtension.class)
class ProyeccionBusesTest {

    @Mock
    VistaBusRepositorio buses;

    @InjectMocks
    ProyeccionBuses proyeccion;

    private final UUID busId = UUID.randomUUID();
    private final UUID sucursalId = UUID.randomUUID();

    @Test
    void guardarHaceUpsertConLaVersionDelEvento() {
        when(buses.guardar(busId, "C1", sucursalId, true, 5L)).thenReturn(1);

        proyeccion.guardar(busId, sucursalId, "C1", "ACTIVO", 5L);

        verify(buses).guardar(busId, "C1", sucursalId, true, 5L);
        verify(buses, never()).save(any());
        verify(buses, never()).findById(any());
    }

    @Test
    void enTallerYEnRutaSiguenActivosEInactivoDesactiva() {
        proyeccion.guardar(busId, sucursalId, "C1", "EN_TALLER", 1L);
        proyeccion.guardar(busId, sucursalId, "C1", "EN_RUTA", 2L);
        proyeccion.guardar(busId, sucursalId, "C1", "INACTIVO", 3L);

        verify(buses).guardar(busId, "C1", sucursalId, true, 1L);
        verify(buses).guardar(busId, "C1", sucursalId, true, 2L);
        verify(buses).guardar(busId, "C1", sucursalId, false, 3L);
    }

    @Test
    void guardarVersionViejaNoFalla() {
        // el mock devuelve 0 filas: la vista ya tiene una versión más nueva
        when(buses.guardar(busId, "C1", sucursalId, true, 1L)).thenReturn(0);

        assertThatCode(() -> proyeccion.guardar(busId, sucursalId, "C1", "ACTIVO", 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void guardarSinVersionNiEstadoUsaCeroYActivo() {
        proyeccion.guardar(busId, sucursalId, "C1", null, null);

        verify(buses).guardar(busId, "C1", sucursalId, true, 0L);
    }

    @Test
    void desactivarConLaVersionDelEvento() {
        when(buses.desactivar(busId, 9L)).thenReturn(1);

        proyeccion.desactivar(busId, 9L);

        verify(buses).desactivar(busId, 9L);
        verify(buses, never()).guardar(any(), any(), any(), anyBoolean(), anyLong());
    }

    @Test
    void desactivarVersionViejaOInexistenteNoFalla() {
        // 0 filas: evento viejo o bus que no está en la vista
        assertThatCode(() -> proyeccion.desactivar(busId, 1L)).doesNotThrowAnyException();
        verify(buses).desactivar(busId, 1L);
    }

    @Test
    void desactivarSinVersionUsaCero() {
        proyeccion.desactivar(busId, null);

        verify(buses).desactivar(busId, 0L);
    }
}
