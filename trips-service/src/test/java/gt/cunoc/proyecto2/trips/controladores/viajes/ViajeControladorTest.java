package gt.cunoc.proyecto2.trips.controladores.viajes;

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
import gt.cunoc.proyecto2.trips.dtos.viajes.ActualizarViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.NuevoViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ViajeControladorTest {

    private static final LocalDate DESDE = FECHA;
    private static final LocalDate HASTA = FECHA.plusDays(10);

    private final ViajeServicio servicio = mock(ViajeServicio.class);
    private final ViajeControlador controlador = new ViajeControlador(servicio);
    private final RutaEntidad ruta = DatosPrueba.ruta();
    private final ViajeEntidad viaje = DatosPrueba.viajeRegular(ruta, DatosPrueba.asignacion());
    private final ViajeRespuesta respuesta = new ViajeRespuesta(viaje, new NombresViaje(Map.of(ruta.getId(), ruta),
            Map.of(), Map.of(XELA, "Xela", GUATE, "Guate"),
            Map.of(viaje.getBusId(), DatosPrueba.vistaBus(viaje.getBusId(), "P-123ABC")),
            Map.of(viaje.getChoferId(), "Juan Perez")));
    private final UUID rutaId = ruta.getId();

    // ----- T8: el filtro depende de quién busca -----

    @Test
    void sinSesionSoloBuscaRegularesProgramados() {
        when(servicio.buscar(any())).thenReturn(List.of(respuesta));

        ResponseEntity<List<ViajeRespuesta>> lista = controlador.buscar(rutaId, GUATE, DESDE, HASTA,
                EstadoViaje.EN_RUTA, TipoViaje.PRIVADO, null);

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).containsExactly(respuesta);
        verify(servicio).buscar(new FiltroViajes(rutaId, GUATE, null, EstadoViaje.PROGRAMADO, TipoViaje.REGULAR,
                DESDE, HASTA));
    }

    @Test
    void unClienteBuscaComoSinSesion() {
        controlador.buscar(null, null, null, null, EstadoViaje.COMPLETADO, null, DatosPrueba.cliente());

        verify(servicio).buscar(new FiltroViajes(null, null, null, EstadoViaje.PROGRAMADO, TipoViaje.REGULAR,
                null, null));
    }

    @Test
    void unChoferEnT8BuscaComoSinSesion() {
        controlador.buscar(rutaId, null, null, null, null, null, DatosPrueba.chofer(UUID.randomUUID()));

        verify(servicio).buscar(new FiltroViajes(rutaId, null, null, EstadoViaje.PROGRAMADO, TipoViaje.REGULAR,
                null, null));
    }

    @Test
    void elAdminDelSistemaBuscaConLosFiltrosTalCual() {
        controlador.buscar(rutaId, GUATE, DESDE, HASTA, EstadoViaje.COMPLETADO, TipoViaje.PRIVADO,
                DatosPrueba.adminSistema());

        verify(servicio).buscar(new FiltroViajes(rutaId, GUATE, null, EstadoViaje.COMPLETADO, TipoViaje.PRIVADO,
                DESDE, HASTA));
    }

    @Test
    void elAdminDeSucursalSinSucursalBuscaEnLaSuya() {
        controlador.buscar(null, null, DESDE, HASTA, EstadoViaje.EN_RUTA, TipoViaje.PRIVADO,
                DatosPrueba.adminSucursal(XELA));

        verify(servicio).buscar(new FiltroViajes(null, XELA, null, EstadoViaje.EN_RUTA, TipoViaje.PRIVADO,
                DESDE, HASTA));
    }

    @Test
    void elCajeroPuedeIndicarSuPropiaSucursal() {
        controlador.buscar(null, XELA, null, null, null, null, DatosPrueba.cajero(XELA));

        verify(servicio).buscar(new FiltroViajes(null, XELA, null, null, null, null, null));
    }

    @Test
    void elPersonalNoBuscaEnOtraSucursal() {
        VerificarError.lanza(() -> controlador.buscar(null, GUATE, null, null, null, null,
                DatosPrueba.adminSucursal(XELA)), "SUCURSAL_AJENA");

        verify(servicio, never()).buscar(any());
    }

    @Test
    void desdeDespuesDeHastaEsRangoInvalido() {
        VerificarError.lanza(() -> controlador.buscar(null, null, HASTA, DESDE, null, null, null),
                ErrorViaje.RANGO_FECHAS_INVALIDO);

        verify(servicio, never()).buscar(any());
    }

    // ----- T13 -----

    @Test
    void miosBuscaLosViajesDelChoferDelToken() {
        UUID choferId = UUID.randomUUID();
        when(servicio.buscar(any())).thenReturn(List.of(respuesta));

        ResponseEntity<List<ViajeRespuesta>> mios = controlador.mios(DESDE, HASTA, EstadoViaje.PROGRAMADO,
                DatosPrueba.chofer(choferId));

        assertThat(mios.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mios.getBody()).containsExactly(respuesta);
        verify(servicio).buscar(new FiltroViajes(null, null, choferId, EstadoViaje.PROGRAMADO, null, DESDE, HASTA));
    }

    @Test
    void miosSinChoferAsociadoEsProhibido() {
        VerificarError.lanza(() -> controlador.mios(null, null, null, DatosPrueba.chofer(null)),
                ErrorViaje.SIN_CHOFER_ASOCIADO);

        verify(servicio, never()).buscar(any());
    }

    // ----- T9 a T12 -----

    @Test
    void obtenerConYSinSesionRespondeOk() {
        ViajeDetalleRespuesta detalle = new ViajeDetalleRespuesta(respuesta, null, null);
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        when(servicio.obtener(viaje.getId(), null)).thenReturn(detalle);
        when(servicio.obtener(viaje.getId(), admin)).thenReturn(detalle);

        ResponseEntity<ViajeDetalleRespuesta> publico = controlador.obtener(viaje.getId(), null);
        ResponseEntity<ViajeDetalleRespuesta> personal = controlador.obtener(viaje.getId(), admin);

        assertThat(publico.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publico.getBody()).isSameAs(detalle);
        assertThat(personal.getBody()).isSameAs(detalle);
    }

    @Test
    void programarResponde201() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        NuevoViajeSolicitud solicitud = new NuevoViajeSolicitud(rutaId, viaje.getBusId(), viaje.getChoferId(),
                DatosPrueba.SALIDA, DatosPrueba.LLEGADA, false, null);
        when(servicio.programar(solicitud, admin)).thenReturn(respuesta);

        ResponseEntity<ViajeRespuesta> programado = controlador.programar(solicitud, admin);

        assertThat(programado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(programado.getBody()).isSameAs(respuesta);
        assertThat(programado.getBody().origenNombre()).isEqualTo("Xela");
        assertThat(programado.getBody().busPlaca()).isEqualTo("P-123ABC");
    }

    @Test
    void actualizarRespondeOk() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        ActualizarViajeSolicitud solicitud = new ActualizarViajeSolicitud(viaje.getBusId(), viaje.getChoferId(),
                DatosPrueba.SALIDA.plusHours(1), DatosPrueba.LLEGADA.plusHours(1), null, 0);
        when(servicio.actualizar(viaje.getId(), solicitud, admin)).thenReturn(respuesta);

        ResponseEntity<ViajeRespuesta> actualizado = controlador.actualizar(viaje.getId(), solicitud, admin);

        assertThat(actualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizado.getBody()).isSameAs(respuesta);
    }

    @Test
    void eliminarResponde204ConElMotivo() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);

        ResponseEntity<Void> eliminado = controlador.eliminar(viaje.getId(), "Bus en mantenimiento", admin);
        ResponseEntity<Void> sinMotivo = controlador.eliminar(viaje.getId(), null, admin);

        assertThat(eliminado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(eliminado.getBody()).isNull();
        assertThat(sinMotivo.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(servicio).eliminar(viaje.getId(), "Bus en mantenimiento", admin);
        verify(servicio).eliminar(viaje.getId(), null, admin);
    }
}
