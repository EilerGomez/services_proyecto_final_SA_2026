package gt.cunoc.proyecto2.trips.controladores.viajes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.viajes.VendibleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.MotivoNoVendible;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ViajeInternoControladorTest {

    private final ViajeServicio servicio = mock(ViajeServicio.class);
    private final ViajeInternoControlador controlador = new ViajeInternoControlador(servicio);

    @Test
    void vendibleSiempreResponde200() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        UUID inexistente = UUID.randomUUID();
        when(servicio.consultarVendible(viaje.getId())).thenReturn(new VendibleRespuesta(viaje, true));
        when(servicio.consultarVendible(inexistente)).thenReturn(VendibleRespuesta.noExiste(inexistente));

        ResponseEntity<VendibleRespuesta> si = controlador.vendible(viaje.getId());
        ResponseEntity<VendibleRespuesta> no = controlador.vendible(inexistente);

        assertThat(si.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(si.getBody().vendible()).isTrue();
        assertThat(si.getBody().precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(no.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(no.getBody().vendible()).isFalse();
        assertThat(no.getBody().motivo()).isEqualTo(MotivoNoVendible.VIAJE_NO_EXISTE);
    }

    @Test
    void activosDeUnBusYDeUnChofer() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        ViajesActivosRespuesta conViajes = new ViajesActivosRespuesta(List.of(viaje));
        ViajesActivosRespuesta sinViajes = new ViajesActivosRespuesta(List.of());
        UUID choferId = UUID.randomUUID();
        when(servicio.viajesActivos(viaje.getBusId(), null)).thenReturn(conViajes);
        when(servicio.viajesActivos(null, choferId)).thenReturn(sinViajes);

        ResponseEntity<ViajesActivosRespuesta> delBus = controlador.activos(viaje.getBusId(), null);
        ResponseEntity<ViajesActivosRespuesta> delChofer = controlador.activos(null, choferId);

        assertThat(delBus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delBus.getBody().cantidad()).isEqualTo(1);
        assertThat(delBus.getBody().viajes()).singleElement().satisfies(v -> {
            assertThat(v.viajeId()).isEqualTo(viaje.getId());
            assertThat(v.estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        });
        assertThat(delChofer.getBody()).isSameAs(sinViajes);
        assertThat(delChofer.getBody().cantidad()).isZero();
    }
}
