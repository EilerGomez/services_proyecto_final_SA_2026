package gt.cunoc.proyecto2.trips.controladores.alquileres;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.alquileres.ConfiguracionAlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevaConfiguracionAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import gt.cunoc.proyecto2.trips.servicios.alquileres.ConfiguracionAlquilerServicio;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ConfiguracionAlquilerControladorTest {

    private final ConfiguracionAlquilerServicio servicio = mock(ConfiguracionAlquilerServicio.class);
    private final ConfiguracionAlquilerControlador controlador = new ConfiguracionAlquilerControlador(servicio);

    @Test
    void listarConvierteElHistorialEnElMismoOrden() {
        ConfiguracionAlquilerEntidad reciente = DatosPrueba.configuracion("15.00", FECHA);
        ConfiguracionAlquilerEntidad anterior = DatosPrueba.configuracion("12.50", FECHA.minusMonths(1));
        when(servicio.listar()).thenReturn(List.of(reciente, anterior));

        ResponseEntity<List<ConfiguracionAlquilerRespuesta>> lista = controlador.listar();

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).extracting(ConfiguracionAlquilerRespuesta::configuracionId)
                .containsExactly(reciente.getId(), anterior.getId());
        assertThat(lista.getBody().get(0).precioPorKm()).isEqualByComparingTo("15.00");
        assertThat(lista.getBody().get(1).vigenteDesde()).isEqualTo(FECHA.minusMonths(1));
    }

    @Test
    void listarSinPreciosDevuelveListaVacia() {
        when(servicio.listar()).thenReturn(List.of());

        assertThat(controlador.listar().getBody()).isEmpty();
    }

    @Test
    void registrarResponde201ConElPrecio() {
        UsuarioActual admin = DatosPrueba.adminSistema();
        NuevaConfiguracionAlquilerSolicitud solicitud = new NuevaConfiguracionAlquilerSolicitud(
                new BigDecimal("13.75"), FECHA);
        ConfiguracionAlquilerEntidad guardada = DatosPrueba.configuracion("13.75", FECHA);
        when(servicio.registrar(solicitud, admin)).thenReturn(guardada);

        ResponseEntity<ConfiguracionAlquilerRespuesta> creada = controlador.registrar(solicitud, admin);

        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creada.getBody().configuracionId()).isEqualTo(guardada.getId());
        assertThat(creada.getBody().precioPorKm()).isEqualByComparingTo("13.75");
        assertThat(creada.getBody().vigenteDesde()).isEqualTo(FECHA);
        assertThat(creada.getBody().creadoPor()).isEqualTo(guardada.getCreadoPor());
    }
}
