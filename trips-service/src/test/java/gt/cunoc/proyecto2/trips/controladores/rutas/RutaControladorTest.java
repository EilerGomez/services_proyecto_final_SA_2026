package gt.cunoc.proyecto2.trips.controladores.rutas;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.rutas.ActualizarRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.NuevaRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.servicios.rutas.RutaServicio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RutaControladorTest {

    private final RutaServicio servicio = mock(RutaServicio.class);
    private final RutaControlador controlador = new RutaControlador(servicio);
    private final UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
    private final RutaEntidad ruta = DatosPrueba.ruta();
    private final RutaRespuesta respuesta = new RutaRespuesta(ruta, Map.of(XELA, "Xela", GUATE, "Guate"));

    @Test
    void listarPasaLosFiltrosYRespondeOk() {
        when(servicio.listar(XELA, true)).thenReturn(List.of(respuesta));

        ResponseEntity<List<RutaRespuesta>> lista = controlador.listar(XELA, true);

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).singleElement().satisfies(r -> {
            assertThat(r.rutaId()).isEqualTo(ruta.getId());
            assertThat(r.origenNombre()).isEqualTo("Xela");
            assertThat(r.destinoNombre()).isEqualTo("Guate");
        });
    }

    @Test
    void listarSinFiltrosPasaNulos() {
        when(servicio.listar(null, null)).thenReturn(List.of());

        assertThat(controlador.listar(null, null).getBody()).isEmpty();
        verify(servicio).listar(null, null);
    }

    @Test
    void obtenerRespondeOk() {
        when(servicio.obtener(ruta.getId())).thenReturn(respuesta);

        ResponseEntity<RutaRespuesta> una = controlador.obtener(ruta.getId());

        assertThat(una.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(una.getBody()).isSameAs(respuesta);
    }

    @Test
    void crearResponde201() {
        NuevaRutaSolicitud solicitud = new NuevaRutaSolicitud(XELA, GUATE, new BigDecimal("205.00"),
                DatosPrueba.PRECIO_BOLETO);
        when(servicio.crear(solicitud, admin)).thenReturn(respuesta);

        ResponseEntity<RutaRespuesta> creada = controlador.crear(solicitud, admin);

        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creada.getBody()).isSameAs(respuesta);
    }

    @Test
    void actualizarRespondeOk() {
        ActualizarRutaSolicitud solicitud = new ActualizarRutaSolicitud(new BigDecimal("210.00"),
                new BigDecimal("80.00"), false, 0);
        when(servicio.actualizar(ruta.getId(), solicitud, admin)).thenReturn(respuesta);

        ResponseEntity<RutaRespuesta> actualizada = controlador.actualizar(ruta.getId(), solicitud, admin);

        assertThat(actualizada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizada.getBody()).isSameAs(respuesta);
    }

    @Test
    void eliminarResponde204SinCuerpo() {
        ResponseEntity<Void> eliminada = controlador.eliminar(ruta.getId(), admin);

        assertThat(eliminada.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(eliminada.getBody()).isNull();
        verify(servicio).eliminar(ruta.getId(), admin);
    }
}
