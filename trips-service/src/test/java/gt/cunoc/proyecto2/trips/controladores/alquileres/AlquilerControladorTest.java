package gt.cunoc.proyecto2.trips.controladores.alquileres;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.servicios.alquileres.AlquilerServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AlquilerControladorTest {

    private static final LocalDate DESDE = FECHA;
    private static final LocalDate HASTA = FECHA.plusDays(30);

    private final AlquilerServicio servicio = mock(AlquilerServicio.class);
    private final AlquilerControlador controlador = new AlquilerControlador(servicio);
    private final UUID clienteId = UUID.randomUUID();
    private final UsuarioActual cliente = new UsuarioActual(clienteId, "cliente@correo.com",
            DatosPrueba.cliente().roles(), null, null);
    private final AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler(clienteId);
    private final AlquilerRespuesta respuesta = new AlquilerRespuesta(alquiler, null);

    @Test
    void solicitarResponde201() {
        NuevoAlquilerSolicitud solicitud = new NuevoAlquilerSolicitud(XELA, null, "Ana López", null,
                "Quetzaltenango", "Antigua Guatemala", OffsetDateTime.parse("2026-11-05T07:00:00-06:00"),
                OffsetDateTime.parse("2026-11-05T19:00:00-06:00"), 30, new BigDecimal("200.00"), FECHA,
                null, null, null, null);
        when(servicio.solicitar(solicitud, cliente)).thenReturn(respuesta);

        ResponseEntity<AlquilerRespuesta> creado = controlador.solicitar(solicitud, cliente);

        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creado.getBody()).isSameAs(respuesta);
        assertThat(creado.getBody().estado()).isEqualTo(EstadoAlquiler.SOLICITADO);
        assertThat(creado.getBody().precioEstimado()).isEqualByComparingTo("2750.00");
    }

    // ----- T17: el filtro depende de quién lista -----

    @Test
    void elAdminDelSistemaListaConLosFiltrosTalCual() {
        when(servicio.listar(any())).thenReturn(List.of(respuesta));

        ResponseEntity<List<AlquilerRespuesta>> lista = controlador.listar(GUATE, EstadoAlquiler.PAGADO, DESDE,
                HASTA, DatosPrueba.adminSistema());

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).containsExactly(respuesta);
        verify(servicio).listar(new FiltroAlquileres(GUATE, EstadoAlquiler.PAGADO, DESDE, HASTA));
    }

    @Test
    void elAdminDelSistemaSinFiltrosListaTodo() {
        controlador.listar(null, null, null, null, DatosPrueba.adminSistema());

        verify(servicio).listar(new FiltroAlquileres(null, null, null, null));
    }

    @Test
    void elAdminDeSucursalSinSucursalListaLaSuya() {
        controlador.listar(null, EstadoAlquiler.SOLICITADO, DESDE, null, DatosPrueba.adminSucursal(XELA));

        verify(servicio).listar(new FiltroAlquileres(XELA, EstadoAlquiler.SOLICITADO, DESDE, null));
    }

    @Test
    void elCajeroPuedeIndicarSuPropiaSucursal() {
        controlador.listar(XELA, null, null, HASTA, DatosPrueba.cajero(XELA));

        verify(servicio).listar(new FiltroAlquileres(XELA, null, null, HASTA));
    }

    @Test
    void elPersonalNoListaOtraSucursal() {
        VerificarError.lanza(() -> controlador.listar(GUATE, null, null, null, DatosPrueba.cajero(XELA)),
                "SUCURSAL_AJENA");

        verify(servicio, never()).listar(any());
    }

    @Test
    void desdeDespuesDeHastaEsRangoInvalido() {
        VerificarError.lanza(() -> controlador.listar(null, null, HASTA, DESDE, DatosPrueba.adminSistema()),
                ErrorViaje.RANGO_FECHAS_INVALIDO);

        verify(servicio, never()).listar(any());
    }

    // ----- T18 a T22 -----

    @Test
    void miosYObtenerRespondenOk() {
        when(servicio.mios(cliente)).thenReturn(List.of(respuesta));
        when(servicio.obtener(alquiler.getId(), cliente)).thenReturn(respuesta);

        ResponseEntity<List<AlquilerRespuesta>> mios = controlador.mios(cliente);
        ResponseEntity<AlquilerRespuesta> uno = controlador.obtener(alquiler.getId(), cliente);

        assertThat(mios.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mios.getBody()).singleElement()
                .satisfies(a -> assertThat(a.clienteUsuarioId()).isEqualTo(clienteId));
        assertThat(uno.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uno.getBody()).isSameAs(respuesta);
    }

    @Test
    void cotizarRespondeOk() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        CotizarAlquilerSolicitud solicitud = new CotizarAlquilerSolicitud(new BigDecimal("2600.00"), 0);
        when(servicio.cotizar(alquiler.getId(), solicitud, admin)).thenReturn(respuesta);

        ResponseEntity<AlquilerRespuesta> cotizado = controlador.cotizar(alquiler.getId(), solicitud, admin);

        assertThat(cotizado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cotizado.getBody()).isSameAs(respuesta);
    }

    @Test
    void cancelarRespondeOk() {
        CancelarAlquilerSolicitud solicitud = new CancelarAlquilerSolicitud("  Cambio de planes ", 0);
        when(servicio.cancelar(alquiler.getId(), solicitud, cliente)).thenReturn(respuesta);

        ResponseEntity<AlquilerRespuesta> cancelado = controlador.cancelar(alquiler.getId(), solicitud, cliente);

        assertThat(cancelado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelado.getBody()).isSameAs(respuesta);
        assertThat(solicitud.motivo()).isEqualTo("Cambio de planes");
    }

    @Test
    void asignarResponde201ConElViajePrivado() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        AlquilerPrivadoEntidad pagado = DatosPrueba.alquilerPagado();
        ViajeEntidad viaje = DatosPrueba.viajePrivado(pagado, DatosPrueba.asignacion());
        ViajeRespuesta viajeRespuesta = new ViajeRespuesta(viaje, new NombresViaje(Map.of(),
                Map.of(pagado.getId(), pagado), Map.of(), Map.of(), Map.of()));
        ViajeDetalleRespuesta detalle = new ViajeDetalleRespuesta(viajeRespuesta, null, null);
        AsignarAlquilerSolicitud solicitud = new AsignarAlquilerSolicitud(viaje.getBusId(), viaje.getChoferId(),
                OffsetDateTime.parse("2026-11-05T20:00:00-06:00"), 0);
        when(servicio.asignar(pagado.getId(), solicitud, admin)).thenReturn(detalle);

        ResponseEntity<ViajeDetalleRespuesta> asignado = controlador.asignar(pagado.getId(), solicitud, admin);

        assertThat(asignado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(asignado.getBody()).isSameAs(detalle);
        assertThat(asignado.getBody().viaje().tipo()).isEqualTo(TipoViaje.PRIVADO);
        assertThat(asignado.getBody().viaje().origenNombre()).isEqualTo("Quetzaltenango");
    }
}
