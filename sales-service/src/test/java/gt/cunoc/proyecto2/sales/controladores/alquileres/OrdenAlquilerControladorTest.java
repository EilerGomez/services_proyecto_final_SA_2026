package gt.cunoc.proyecto2.sales.controladores.alquileres;

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
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.servicios.alquileres.OrdenAlquilerServicio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrdenAlquilerControladorTest {

    private final OrdenAlquilerServicio servicio = mock(OrdenAlquilerServicio.class);
    private final OrdenAlquilerControlador controlador = new OrdenAlquilerControlador(servicio);
    private final UUID clienteId = UUID.randomUUID();
    private final UsuarioActual cliente = DatosPrueba.cliente(clienteId);
    private final OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
    private final OrdenAlquilerRespuesta respuesta = new OrdenAlquilerRespuesta(orden);
    private final PagoOrdenSolicitud pago = new PagoOrdenSolicitud(FECHA_PAGO);

    // ----- S9: el filtro depende de quién lista -----

    @Test
    void elAdminDelSistemaListaConLosFiltrosTalCual() {
        when(servicio.listar(any())).thenReturn(List.of(respuesta));

        ResponseEntity<List<OrdenAlquilerRespuesta>> lista = controlador.listar(GUATE, EstadoOrdenAlquiler.PAGADA,
                DatosPrueba.adminSistema());

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).containsExactly(respuesta);
        verify(servicio).listar(new FiltroOrdenes(GUATE, EstadoOrdenAlquiler.PAGADA));
    }

    @Test
    void elAdminDelSistemaSinFiltrosListaTodo() {
        controlador.listar(null, null, DatosPrueba.adminSistema());

        verify(servicio).listar(new FiltroOrdenes(null, null));
    }

    @Test
    void elAdminDeSucursalSinSucursalListaLaSuya() {
        controlador.listar(null, EstadoOrdenAlquiler.PENDIENTE_PAGO, DatosPrueba.adminSucursal(XELA));

        verify(servicio).listar(new FiltroOrdenes(XELA, EstadoOrdenAlquiler.PENDIENTE_PAGO));
    }

    @Test
    void elCajeroPuedeIndicarSuPropiaSucursal() {
        controlador.listar(XELA, null, DatosPrueba.cajero(XELA));

        verify(servicio).listar(new FiltroOrdenes(XELA, null));
    }

    @Test
    void elPersonalNoListaOtraSucursal() {
        VerificarError.lanza(() -> controlador.listar(GUATE, null, DatosPrueba.adminSucursal(XELA)),
                "SUCURSAL_AJENA");

        verify(servicio, never()).listar(any());
    }

    // ----- S10 y S11 -----

    @Test
    void miasYObtenerRespondenOk() {
        when(servicio.mias(cliente)).thenReturn(List.of(respuesta));
        when(servicio.obtener(orden.getId(), cliente)).thenReturn(respuesta);

        ResponseEntity<List<OrdenAlquilerRespuesta>> mias = controlador.mias(cliente);
        ResponseEntity<OrdenAlquilerRespuesta> una = controlador.obtener(orden.getId(), cliente);

        assertThat(mias.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mias.getBody()).singleElement()
                .satisfies(o -> assertThat(o.ordenId()).isEqualTo(orden.getId()));
        assertThat(una.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(una.getBody()).isSameAs(respuesta);
        assertThat(una.getBody().monto()).isEqualByComparingTo("2600.00");
    }

    // ----- S12 -----

    @Test
    void pagarConCarteraResponde202() {
        when(servicio.pagarConCartera(orden.getId(), pago, cliente)).thenReturn(respuesta);

        ResponseEntity<OrdenAlquilerRespuesta> aceptada = controlador.pagarConCartera(orden.getId(), pago, cliente);

        assertThat(aceptada.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(aceptada.getBody()).isSameAs(respuesta);
        assertThat(aceptada.getBody().estado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        verify(servicio).pagarConCartera(orden.getId(), pago, cliente);
    }

    // ----- S13 -----

    @Test
    void pagarEnEfectivoRespondeOk() {
        UsuarioActual cajero = DatosPrueba.cajero(XELA);
        when(servicio.pagarEnEfectivo(orden.getId(), pago, cajero)).thenReturn(respuesta);

        ResponseEntity<OrdenAlquilerRespuesta> pagada = controlador.pagarEnEfectivo(orden.getId(), pago, cajero);

        assertThat(pagada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pagada.getBody()).isSameAs(respuesta);
        verify(servicio).pagarEnEfectivo(orden.getId(), pago, cajero);
    }
}
