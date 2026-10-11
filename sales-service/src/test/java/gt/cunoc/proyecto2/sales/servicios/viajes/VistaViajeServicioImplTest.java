package gt.cunoc.proyecto2.sales.servicios.viajes;

import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VistaViajeServicioImplTest {

    private final VistaViajeRepositorio repositorio = mock(VistaViajeRepositorio.class);

    private final VistaViajeServicioImpl servicio = new VistaViajeServicioImpl(repositorio);

    @Test
    void unViajeQueNoEstaEnLaVistaNoEstaDisponible() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.obtenerALaVenta(id), ErrorViaje.VIAJE_NO_DISPONIBLE);
    }

    @Test
    void unViajeEnRutaYaNoEstaALaVenta() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id))
                .thenReturn(Optional.of(DatosPrueba.vistaViaje(id, XELA, 40, EstadoViaje.EN_RUTA)));

        VerificarError.lanza(() -> servicio.obtenerALaVenta(id), ErrorViaje.VIAJE_NO_DISPONIBLE);
    }

    @Test
    void unViajeProgramadoSeDevuelve() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        when(repositorio.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));

        assertThat(servicio.obtenerALaVenta(viaje.getViajeId())).isSameAs(viaje);
    }
}
