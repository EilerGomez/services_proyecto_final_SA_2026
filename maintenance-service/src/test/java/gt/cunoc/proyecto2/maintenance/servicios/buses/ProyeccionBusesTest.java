package gt.cunoc.proyecto2.maintenance.servicios.buses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyeccionBusesTest {

    @Mock
    VistaBusRepositorio buses;

    @InjectMocks
    ProyeccionBuses proyeccion;

    @Test
    void guardarCreaActualizaYDescartaVersionVieja() {
        UUID busId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(buses.findById(busId)).thenReturn(Optional.empty());
        proyeccion.guardar(busId, sucursalId, "C1", "EN_TALLER", 0L);

        VistaBusEntidad guardado = new VistaBusEntidad();
        guardado.setBusId(busId);
        guardado.setVersion(3);
        when(buses.findById(busId)).thenReturn(Optional.of(guardado));
        proyeccion.guardar(busId, sucursalId, "C1", "INACTIVO", 5L);
        assertThat(guardado.isActivo()).isFalse();
        proyeccion.guardar(busId, sucursalId, "C1", "ACTIVO", 1L); // vieja
        assertThat(guardado.isActivo()).isFalse();
        proyeccion.guardar(busId, sucursalId, "C1", "ACTIVO", null); // sin versión aplica

        verify(buses, org.mockito.Mockito.times(3)).save(any(VistaBusEntidad.class));
    }

    @Test
    void desactivarApagaYDescartaVersionVieja() {
        UUID busId = UUID.randomUUID();
        when(buses.findById(busId)).thenReturn(Optional.empty());
        proyeccion.desactivar(busId, 1L); // no existe: no hace nada

        VistaBusEntidad guardado = new VistaBusEntidad();
        guardado.setBusId(busId);
        guardado.setActivo(true);
        guardado.setVersion(4);
        when(buses.findById(busId)).thenReturn(Optional.of(guardado));
        proyeccion.desactivar(busId, 1L); // vieja
        assertThat(guardado.isActivo()).isTrue();
        proyeccion.desactivar(busId, 9L);
        assertThat(guardado.isActivo()).isFalse();
        assertThat(guardado.getVersion()).isEqualTo(9);

        verify(buses).save(any(VistaBusEntidad.class));
    }

    @Test
    void guardarSinVersionNoRompe() {
        UUID busId = UUID.randomUUID();
        when(buses.findById(busId)).thenReturn(Optional.empty());
        proyeccion.guardar(busId, UUID.randomUUID(), "C1", null, null);

        var captor = org.mockito.ArgumentCaptor.forClass(VistaBusEntidad.class);
        verify(buses).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isTrue();
    }
}
