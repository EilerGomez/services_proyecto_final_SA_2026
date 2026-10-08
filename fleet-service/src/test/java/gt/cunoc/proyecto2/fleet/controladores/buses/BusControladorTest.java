package gt.cunoc.proyecto2.fleet.controladores.buses;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.buses.ActualizarBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.BusDetalleRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.BusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.CambiarEstadoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.NuevoBusSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.servicios.bitacora.BitacoraKilometrajeServicio;
import gt.cunoc.proyecto2.fleet.servicios.buses.BusServicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BusControladorTest {

    private final BusServicio servicio = mock(BusServicio.class);
    private final BitacoraKilometrajeServicio bitacora = mock(BitacoraKilometrajeServicio.class);
    private final BusControlador controlador = new BusControlador(servicio, bitacora);
    private final UUID sucursalId = UUID.randomUUID();
    private final UsuarioActual admin = DatosPrueba.adminSucursal(sucursalId);
    private final BusEntidad bus = DatosPrueba.bus(sucursalId);

    @Test
    void listarConvierteLasEntidades() {
        when(servicio.listar(null, EstadoBus.ACTIVO, admin)).thenReturn(List.of(bus));

        ResponseEntity<List<BusRespuesta>> respuesta = controlador.listar(null, EstadoBus.ACTIVO, admin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).extracting(BusRespuesta::placa).containsExactly("P-123ABC");
    }

    @Test
    void obtenerIncluyeLosUltimosViajes() {
        UUID viajeId = UUID.randomUUID();
        BitacoraKilometrajeEntidad viaje = BitacoraKilometrajeEntidad.registrar(bus.getId(), viajeId,
                new BigDecimal("900.00"), new BigDecimal("1000.00"), OffsetDateTime.parse("2026-10-19T17:00:00-06:00"));
        when(servicio.obtener(bus.getId(), admin)).thenReturn(bus);
        when(bitacora.ultimosViajes(bus.getId())).thenReturn(List.of(viaje));

        BusDetalleRespuesta detalle = controlador.obtener(bus.getId(), admin).getBody();

        assertThat(detalle.bus().busId()).isEqualTo(bus.getId());
        assertThat(detalle.ultimosViajes()).singleElement().satisfies(v -> {
            assertThat(v.viajeId()).isEqualTo(viajeId);
            assertThat(v.kmRecorridos()).isEqualByComparingTo("100.00");
        });
    }

    @Test
    void crearResponde201() {
        NuevoBusSolicitud solicitud = new NuevoBusSolicitud(sucursalId, "P-123ABC", "Volvo", "B12", 2018, 50,
                null, DatosPrueba.FOTO_BUS, FECHA);
        BusRespuesta creado = new BusRespuesta(bus);
        when(servicio.crear(solicitud, admin)).thenReturn(creado);

        ResponseEntity<BusRespuesta> respuesta = controlador.crear(solicitud, admin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(creado);
    }

    @Test
    void actualizarYCambiarEstadoRespondenOk() {
        ActualizarBusSolicitud actualizar = new ActualizarBusSolicitud("Volvo", "B12", 2018, 50, DatosPrueba.FOTO_BUS, 0);
        CambiarEstadoBusSolicitud estado = new CambiarEstadoBusSolicitud(EstadoBus.EN_TALLER, FECHA, 0);
        BusRespuesta respuesta = new BusRespuesta(bus);
        when(servicio.actualizar(bus.getId(), actualizar, admin)).thenReturn(respuesta);
        when(servicio.cambiarEstado(bus.getId(), estado, admin)).thenReturn(respuesta);

        assertThat(controlador.actualizar(bus.getId(), actualizar, admin).getBody()).isSameAs(respuesta);
        assertThat(controlador.cambiarEstado(bus.getId(), estado, admin).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void disponibilidadInterna() {
        BusInternoControlador interno = new BusInternoControlador(servicio);
        DisponibilidadBusRespuesta disponibilidad = new DisponibilidadBusRespuesta(bus);
        when(servicio.consultarDisponibilidad(bus.getId())).thenReturn(disponibilidad);

        assertThat(interno.disponibilidad(bus.getId()).getBody()).isSameAs(disponibilidad);
    }
}
