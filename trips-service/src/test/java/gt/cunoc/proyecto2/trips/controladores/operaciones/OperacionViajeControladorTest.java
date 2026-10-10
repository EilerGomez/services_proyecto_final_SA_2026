package gt.cunoc.proyecto2.trips.controladores.operaciones;

import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.operaciones.LlegadaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarLlegadaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarSalidaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.SalidaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.servicios.operaciones.OperacionViajeServicio;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OperacionViajeControladorTest {

    private final OperacionViajeServicio servicio = mock(OperacionViajeServicio.class);
    private final OperacionViajeControlador controlador = new OperacionViajeControlador(servicio);
    private final ViajeEntidad viaje = DatosPrueba.viajeRegular();
    private final SalidaViajeEntidad salida = DatosPrueba.salida(viaje);
    private final ViajeRespuesta respuesta = new ViajeRespuesta(viaje,
            new NombresViaje(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));

    @Test
    void registrarSalidaRespondeOkConLaSalida() {
        UsuarioActual chofer = DatosPrueba.chofer(viaje.getChoferId());
        RegistrarSalidaSolicitud solicitud = new RegistrarSalidaSolicitud(DatosPrueba.SALIDA.plusMinutes(5),
                new BigDecimal("1000.00"));
        ViajeDetalleRespuesta detalle = new ViajeDetalleRespuesta(respuesta, new SalidaRespuesta(salida), null);
        when(servicio.registrarSalida(viaje.getId(), solicitud, chofer)).thenReturn(detalle);

        ResponseEntity<ViajeDetalleRespuesta> registrada = controlador.registrarSalida(viaje.getId(), solicitud,
                chofer);

        assertThat(registrada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registrada.getBody()).isSameAs(detalle);
        assertThat(registrada.getBody().salida().kmInicial()).isEqualByComparingTo("1000.00");
        assertThat(registrada.getBody().llegada()).isNull();
    }

    @Test
    void registrarLlegadaRespondeOkConLosCostos() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        LlegadaViajeEntidad llegada = DatosPrueba.llegada(viaje, salida);
        RegistrarLlegadaSolicitud solicitud = new RegistrarLlegadaSolicitud(DatosPrueba.LLEGADA.plusMinutes(10),
                new BigDecimal("1207.50"), new BigDecimal("950.00"));
        ViajeDetalleRespuesta detalle = new ViajeDetalleRespuesta(respuesta, new SalidaRespuesta(salida),
                new LlegadaRespuesta(llegada));
        UUID viajeId = viaje.getId();
        when(servicio.registrarLlegada(viajeId, solicitud, admin)).thenReturn(detalle);

        ResponseEntity<ViajeDetalleRespuesta> registrada = controlador.registrarLlegada(viajeId, solicitud, admin);

        assertThat(registrada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registrada.getBody()).isSameAs(detalle);
        assertThat(registrada.getBody().llegada().kmRecorridos()).isEqualByComparingTo("207.50");
    }
}
