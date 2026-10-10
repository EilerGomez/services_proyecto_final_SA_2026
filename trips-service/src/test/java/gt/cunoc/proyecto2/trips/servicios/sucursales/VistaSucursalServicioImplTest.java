package gt.cunoc.proyecto2.trips.servicios.sucursales;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import gt.cunoc.proyecto2.trips.repositorios.sucursales.VistaSucursalRepositorio;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VistaSucursalServicioImplTest {

    private final VistaSucursalRepositorio repositorio = mock(VistaSucursalRepositorio.class);

    private VistaSucursalServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new VistaSucursalServicioImpl(repositorio);
    }

    @Test
    void verificarUnaSucursalQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.verificarActiva(id), ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    @Test
    void verificarUnaSucursalInactiva() {
        when(repositorio.findById(XELA)).thenReturn(Optional.of(DatosPrueba.vistaSucursal(XELA, "Xela", false)));

        VerificarError.lanza(() -> servicio.verificarActiva(XELA), ErrorSucursal.SUCURSAL_INACTIVA);
    }

    @Test
    void verificarUnaSucursalActivaLaDevuelve() {
        VistaSucursalEntidad xela = DatosPrueba.vistaSucursal(XELA, "Xela", true);
        when(repositorio.findById(XELA)).thenReturn(Optional.of(xela));

        assertThat(servicio.verificarActiva(XELA)).isSameAs(xela);
    }

    @Test
    void nombresDevuelveElNombreDeCadaSucursal() {
        List<UUID> ids = List.of(XELA, GUATE);
        when(repositorio.findAllById(ids)).thenReturn(List.of(
                DatosPrueba.vistaSucursal(XELA, "Xela", true), DatosPrueba.vistaSucursal(GUATE, "Guate", false)));

        Map<UUID, String> nombres = servicio.nombres(ids);

        assertThat(nombres).containsOnly(Map.entry(XELA, "Xela"), Map.entry(GUATE, "Guate"));
    }

    @Test
    void nombresSinSucursalesDevuelveUnMapaVacio() {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(repositorio.findAllById(ids)).thenReturn(List.of());

        assertThat(servicio.nombres(ids)).isEmpty();
    }
}
