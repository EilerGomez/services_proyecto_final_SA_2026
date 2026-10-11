package gt.cunoc.proyecto2.sales.controladores.historial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.dtos.historial.HistorialRespuesta;
import gt.cunoc.proyecto2.sales.servicios.historial.HistorialServicio;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HistorialControladorTest {

    private final HistorialServicio servicio = mock(HistorialServicio.class);
    private final HistorialControlador controlador = new HistorialControlador(servicio);
    private final HistorialRespuesta historial = new HistorialRespuesta("1234567-8", "Ana López", List.of(),
            List.of());

    // ----- S7 -----

    @Test
    void mioDelegaConElUsuarioDelToken() {
        UsuarioActual cliente = DatosPrueba.cliente();
        when(servicio.delCliente(cliente)).thenReturn(historial);

        ResponseEntity<HistorialRespuesta> mio = controlador.mio(cliente);

        assertThat(mio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mio.getBody()).isSameAs(historial);
        verify(servicio).delCliente(cliente);
    }

    // ----- S8 -----

    @Test
    void porNitDelegaConElNitPedido() {
        when(servicio.porNit("1234567-8")).thenReturn(historial);

        ResponseEntity<HistorialRespuesta> delNit = controlador.porNit("1234567-8");

        assertThat(delNit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delNit.getBody()).isSameAs(historial);
        assertThat(delNit.getBody().nit()).isEqualTo("1234567-8");
        assertThat(delNit.getBody().nombre()).isEqualTo("Ana López");
        verify(servicio).porNit("1234567-8");
    }
}
