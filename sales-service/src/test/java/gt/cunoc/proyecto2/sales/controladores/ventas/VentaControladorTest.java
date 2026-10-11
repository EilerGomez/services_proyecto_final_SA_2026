package gt.cunoc.proyecto2.sales.controladores.ventas;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.dtos.ventas.CancelarVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaEnLineaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaPendienteRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.ViajeResumenRespuesta;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class VentaControladorTest {

    private static final LocalDate DESDE = FECHA_PAGO;
    private static final LocalDate HASTA = FECHA_PAGO.plusDays(10);

    private final VentaServicio servicio = mock(VentaServicio.class);
    private final VentaControlador controlador = new VentaControlador(servicio);
    private final VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
    private final UUID viajeId = viaje.getViajeId();
    private final UUID clienteId = UUID.randomUUID();
    private final UsuarioActual cliente = DatosPrueba.cliente(clienteId);
    private final VentaEntidad mostrador = DatosPrueba.ventaMostrador(viaje, 2);
    private final VentaRespuesta respuesta = new VentaRespuesta(mostrador, DatosPrueba.boletos(mostrador, 5, 6),
            new ViajeResumenRespuesta(viaje));

    // ----- S2 -----

    @Test
    void venderEnLineaResponde202ConLaVentaPendiente() {
        NuevaVentaEnLineaSolicitud solicitud = new NuevaVentaEnLineaSolicitud(viajeId, List.of(3, 4), FECHA_PAGO);
        VentaEntidad enLinea = DatosPrueba.ventaEnLinea(viaje, clienteId, 2);
        OffsetDateTime reservadoHasta = OffsetDateTime.now().plusMinutes(5);
        VentaPendienteRespuesta pendiente = new VentaPendienteRespuesta(enLinea, List.of(3, 4), reservadoHasta);
        when(servicio.venderEnLinea(solicitud, cliente)).thenReturn(pendiente);

        ResponseEntity<VentaPendienteRespuesta> aceptada = controlador.venderEnLinea(solicitud, cliente);

        assertThat(aceptada.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(aceptada.getBody()).isSameAs(pendiente);
        assertThat(aceptada.getBody().estado()).isEqualTo(EstadoVenta.PENDIENTE_PAGO);
        assertThat(aceptada.getBody().montoTotal()).isEqualByComparingTo("150.00");
        assertThat(aceptada.getBody().asientos()).containsExactly(3, 4);
        verify(servicio).venderEnLinea(solicitud, cliente);
    }

    // ----- S4 -----

    @Test
    void venderEnMostradorResponde201() {
        UsuarioActual cajero = DatosPrueba.cajero(XELA);
        NuevaVentaMostradorSolicitud solicitud = new NuevaVentaMostradorSolicitud(viajeId, List.of(5, 6), null,
                null, null, FECHA_PAGO);
        when(servicio.venderEnMostrador(solicitud, cajero)).thenReturn(respuesta);

        ResponseEntity<VentaRespuesta> creada = controlador.venderEnMostrador(solicitud, cajero);

        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creada.getBody()).isSameAs(respuesta);
        assertThat(creada.getBody().canal()).isEqualTo(CanalVenta.PRESENCIAL);
        assertThat(creada.getBody().estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(creada.getBody().boletos()).hasSize(2);
        assertThat(creada.getBody().viaje().busPlaca()).isEqualTo("P-123ABC");
        verify(servicio).venderEnMostrador(solicitud, cajero);
    }

    // ----- S5 -----

    @Test
    void cancelarRespondeOkConLaSolicitudNormalizada() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        CancelarVentaSolicitud solicitud = new CancelarVentaSolicitud("  Viaje reprogramado ", FECHA_PAGO, 0);
        when(servicio.cancelar(mostrador.getId(), solicitud, admin)).thenReturn(respuesta);

        ResponseEntity<VentaRespuesta> cancelada = controlador.cancelar(mostrador.getId(), solicitud, admin);

        assertThat(cancelada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelada.getBody()).isSameAs(respuesta);
        assertThat(solicitud.motivo()).isEqualTo("Viaje reprogramado");
        verify(servicio).cancelar(mostrador.getId(), solicitud, admin);
    }

    // ----- S3 -----

    @Test
    void obtenerRespondeOk() {
        when(servicio.obtener(mostrador.getId(), cliente)).thenReturn(respuesta);

        ResponseEntity<VentaRespuesta> una = controlador.obtener(mostrador.getId(), cliente);

        assertThat(una.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(una.getBody()).isSameAs(respuesta);
        assertThat(una.getBody().ventaId()).isEqualTo(mostrador.getId());
        verify(servicio).obtener(mostrador.getId(), cliente);
    }

    // ----- S6: el filtro depende de quién lista -----

    @Test
    void elAdminDelSistemaListaConLosFiltrosTalCual() {
        when(servicio.listar(any())).thenReturn(List.of(respuesta));

        ResponseEntity<List<VentaRespuesta>> lista = controlador.listar(viajeId, GUATE, EstadoVenta.CONFIRMADA,
                CanalVenta.EN_LINEA, DESDE, HASTA, DatosPrueba.adminSistema());

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).containsExactly(respuesta);
        verify(servicio).listar(new FiltroVentas(viajeId, GUATE, EstadoVenta.CONFIRMADA, CanalVenta.EN_LINEA,
                DESDE, HASTA));
    }

    @Test
    void elAdminDelSistemaSinFiltrosListaTodo() {
        controlador.listar(null, null, null, null, null, null, DatosPrueba.adminSistema());

        verify(servicio).listar(new FiltroVentas(null, null, null, null, null, null));
    }

    @Test
    void elAdminDeSucursalSinSucursalListaLaSuya() {
        controlador.listar(null, null, EstadoVenta.CANCELADA, null, DESDE, null, DatosPrueba.adminSucursal(XELA));

        verify(servicio).listar(new FiltroVentas(null, XELA, EstadoVenta.CANCELADA, null, DESDE, null));
    }

    @Test
    void elCajeroPuedeIndicarSuPropiaSucursal() {
        controlador.listar(viajeId, XELA, null, CanalVenta.PRESENCIAL, null, HASTA, DatosPrueba.cajero(XELA));

        verify(servicio).listar(new FiltroVentas(viajeId, XELA, null, CanalVenta.PRESENCIAL, null, HASTA));
    }

    @Test
    void elPersonalNoListaOtraSucursal() {
        VerificarError.lanza(() -> controlador.listar(null, GUATE, null, null, null, null,
                DatosPrueba.cajero(XELA)), "SUCURSAL_AJENA");

        verify(servicio, never()).listar(any());
    }

    @Test
    void desdeDespuesDeHastaEsRangoInvalido() {
        VerificarError.lanza(() -> controlador.listar(null, null, null, null, HASTA, DESDE,
                DatosPrueba.adminSistema()), ErrorVenta.RANGO_FECHAS_INVALIDO);

        verify(servicio, never()).listar(any());
    }
}
