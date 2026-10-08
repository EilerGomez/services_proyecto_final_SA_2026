package gt.cunoc.proyecto2.fleet.controladores.sucursales;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalCreadaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.servicios.sucursales.SucursalServicio;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SucursalControladorTest {

    private final SucursalServicio servicio = mock(SucursalServicio.class);
    private final SucursalControlador controlador = new SucursalControlador(servicio);
    private final SucursalEntidad sucursal = DatosPrueba.sucursal(true);

    @Test
    void listarYObtenerRespondenOk() {
        when(servicio.listar(true)).thenReturn(List.of(sucursal));
        when(servicio.obtenerPorId(sucursal.getId())).thenReturn(sucursal);

        ResponseEntity<List<SucursalRespuesta>> lista = controlador.listar(true);
        ResponseEntity<SucursalRespuesta> una = controlador.obtener(sucursal.getId());

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).extracting(SucursalRespuesta::codigo).containsExactly("XELA");
        assertThat(una.getBody().sucursalId()).isEqualTo(sucursal.getId());
    }

    @Test
    void crearResponde201() {
        NuevaSucursalSolicitud solicitud = new NuevaSucursalSolicitud("XELA", "Xela Central", "Quetzaltenango",
                "Quetzaltenango", "4a calle", null, BigDecimal.ONE, BigDecimal.ONE, FECHA,
                DatosPrueba.cuenta(null, null));
        SucursalCreadaRespuesta creada = new SucursalCreadaRespuesta(new SucursalRespuesta(sucursal),
                new PersonalSucursalRespuesta(DatosPrueba.personal(sucursal.getId(), RolPersonal.ADMIN_SUCURSAL)));
        when(servicio.crear(solicitud)).thenReturn(creada);

        ResponseEntity<SucursalCreadaRespuesta> respuesta = controlador.crear(solicitud);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(creada);
    }

    @Test
    void actualizarYCambiarEstadoRespondenOk() {
        ActualizarSucursalSolicitud actualizar = new ActualizarSucursalSolicitud("Xela", "Q", "Q", "D", null,
                BigDecimal.ONE, BigDecimal.ONE, 0);
        CambiarEstadoSucursalSolicitud estado = new CambiarEstadoSucursalSolicitud(false, 0);
        SucursalRespuesta respuesta = new SucursalRespuesta(sucursal);
        when(servicio.actualizar(sucursal.getId(), actualizar)).thenReturn(respuesta);
        when(servicio.cambiarEstado(sucursal.getId(), estado)).thenReturn(respuesta);

        assertThat(controlador.actualizar(sucursal.getId(), actualizar).getBody()).isSameAs(respuesta);
        assertThat(controlador.cambiarEstado(sucursal.getId(), estado).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
