package gt.cunoc.proyecto2.sales.controladores.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ClientesControladorTest {

    private final VistaClienteRepositorio repositorio = mock(VistaClienteRepositorio.class);
    private final ClientesControlador controlador = new ClientesControlador(repositorio);

    @Test
    void buscarSinTextoDevuelveLosPrimerosClientes() {
        VistaClienteEntidad cliente = mock(VistaClienteEntidad.class);
        UUID usuarioId = UUID.randomUUID();
        when(cliente.getUsuarioId()).thenReturn(usuarioId);
        when(cliente.getNit()).thenReturn("1234567-8");
        when(cliente.getNombre()).thenReturn("Ana López");
        when(cliente.getCorreo()).thenReturn("ana@correo.com");
        when(repositorio.findTop20ByOrderByNombreAsc()).thenReturn(List.of(cliente));

        var respuesta = controlador.buscar(" ");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(
                new gt.cunoc.proyecto2.sales.dtos.clientes.ClienteCuentaRespuesta(
                        usuarioId, "1234567-8", "Ana López", "ana@correo.com"));
        verify(repositorio).findTop20ByOrderByNombreAsc();
    }

    @Test
    void buscarConTextoBuscaEnNitNombreYCorreo() {
        when(repositorio
                .findTop20ByNitContainingIgnoreCaseOrNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(
                        "1234567-8", "1234567-8", "1234567-8"))
                .thenReturn(List.of());

        var respuesta = controlador.buscar(" 1234567-8 ");

        assertThat(respuesta.getBody()).isEmpty();
        verify(repositorio)
                .findTop20ByNitContainingIgnoreCaseOrNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(
                        "1234567-8", "1234567-8", "1234567-8");
    }
}
