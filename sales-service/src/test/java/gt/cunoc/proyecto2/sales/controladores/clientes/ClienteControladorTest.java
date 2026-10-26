package gt.cunoc.proyecto2.sales.controladores.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.dtos.clientes.ClienteRespuesta;
import gt.cunoc.proyecto2.sales.servicios.clientes.ClienteServicio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ClienteControladorTest {

    private final ClienteServicio servicio = mock(ClienteServicio.class);
    private final ClienteControlador controlador = new ClienteControlador(servicio);

    private final ClienteRespuesta ana =
            new ClienteRespuesta(UUID.randomUUID(), "1234567-8", "Ana López", "ana@correo.com");

    @Test
    void buscarRespondeOkConLosClientes() {
        when(servicio.buscar("ana")).thenReturn(List.of(ana));

        ResponseEntity<List<ClienteRespuesta>> encontrados = controlador.buscar("ana");

        assertThat(encontrados.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(encontrados.getBody()).containsExactly(ana);
        verify(servicio).buscar("ana");
    }

    @Test
    void sinBusquedaPideLosPrimeros() {
        when(servicio.buscar(null)).thenReturn(List.of(ana));

        assertThat(controlador.buscar(null).getBody()).containsExactly(ana);

        verify(servicio).buscar(null);
    }
}
