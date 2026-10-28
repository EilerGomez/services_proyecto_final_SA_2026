package gt.cunoc.proyecto2.sales.controladores.tarifas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.dtos.tarifas.ActualizarTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.NuevaTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.tarifas.TarifaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.servicios.tarifas.TarifaReembolsoServicio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TarifaReembolsoControladorTest {

    private final TarifaReembolsoServicio servicio = mock(TarifaReembolsoServicio.class);
    private final TarifaReembolsoControlador controlador = new TarifaReembolsoControlador(servicio);

    private static TarifaReembolsoRespuesta fila() {
        return new TarifaReembolsoRespuesta(UUID.randomUUID(), EstadoAlquilerTarifa.ASIGNADO, 24, true,
                new BigDecimal("80.00"), "Con 24 horas o más", true, 0);
    }

    @Test
    void listarDevuelveLaTabla() {
        when(servicio.listar()).thenReturn(List.of(fila()));

        ResponseEntity<List<TarifaReembolsoRespuesta>> respuesta = controlador.listar();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void aplicableResuelveLaTarifaDelEstadoYLasHoras() {
        ReglaReembolsoRespuesta regla = new ReglaReembolsoRespuesta(UUID.randomUUID(), true,
                new BigDecimal("80.00"), "Con 24 horas o más", 24);
        when(servicio.aplicable(EstadoAlquilerTarifa.ASIGNADO, 30)).thenReturn(regla);

        ResponseEntity<ReglaReembolsoRespuesta> respuesta =
                controlador.aplicable(EstadoAlquilerTarifa.ASIGNADO, 30);

        assertThat(respuesta.getBody()).isEqualTo(regla);
    }

    @Test
    void crearDevuelveCreated() {
        NuevaTarifaReembolsoSolicitud solicitud = new NuevaTarifaReembolsoSolicitud(
                EstadoAlquilerTarifa.PAGADO, 0, true, new BigDecimal("95.00"), "Se retiene el 5%");
        when(servicio.crear(solicitud)).thenReturn(fila());

        ResponseEntity<TarifaReembolsoRespuesta> respuesta = controlador.crear(solicitud);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(servicio).crear(solicitud);
    }

    @Test
    void actualizarDelegaConElId() {
        UUID id = UUID.randomUUID();
        ActualizarTarifaReembolsoSolicitud solicitud = new ActualizarTarifaReembolsoSolicitud(true,
                new BigDecimal("70.00"), "Nuevo texto", true, 0);
        when(servicio.actualizar(id, solicitud)).thenReturn(fila());

        ResponseEntity<TarifaReembolsoRespuesta> respuesta = controlador.actualizar(id, solicitud);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(servicio).actualizar(id, solicitud);
    }
}
