package gt.cunoc.proyecto2.sales.servicios.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.dtos.clientes.ClienteRespuesta;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class ClienteServicioImplTest {

    private final VistaClienteRepositorio repositorio = mock(VistaClienteRepositorio.class);
    private final ClienteServicioImpl servicio = new ClienteServicioImpl(repositorio);

    private final VistaClienteEntidad ana = DatosPrueba.vistaCliente(UUID.randomUUID());

    @Test
    void sinTextoDevuelveLosPrimerosEnOrdenAlfabetico() {
        when(repositorio.findAllByOrderByNombreAsc(any(Pageable.class))).thenReturn(List.of(ana));

        List<ClienteRespuesta> encontrados = servicio.buscar(null);

        assertThat(encontrados).singleElement().satisfies(c -> {
            assertThat(c.usuarioId()).isEqualTo(ana.getUsuarioId());
            assertThat(c.nit()).isEqualTo("1234567-8");
            assertThat(c.nombre()).isEqualTo("Ana López");
            assertThat(c.correo()).isEqualTo("ana@correo.com");
        });
        verify(repositorio, never()).buscar(any(), any());
    }

    @Test
    void elTextoEnBlancoCuentaComoBusquedaVacia() {
        when(repositorio.findAllByOrderByNombreAsc(any(Pageable.class))).thenReturn(List.of());

        assertThat(servicio.buscar("   ")).isEmpty();

        verify(repositorio, never()).buscar(any(), any());
    }

    @Test
    void buscaEnMayusculasYConComodinesAAmbosLados() {
        when(repositorio.buscar(any(), any(Pageable.class))).thenReturn(List.of(ana));

        assertThat(servicio.buscar(" ana ")).hasSize(1);

        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        verify(repositorio).buscar(patron.capture(), any(Pageable.class));
        assertThat(patron.getValue()).isEqualTo("%ANA%");
    }

    @Test
    void nuncaPideMasDeVeinteClientes() {
        when(repositorio.buscar(any(), any(Pageable.class))).thenReturn(List.of());

        servicio.buscar("123");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscar(any(), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(pagina.getValue().getPageNumber()).isZero();
    }
}
