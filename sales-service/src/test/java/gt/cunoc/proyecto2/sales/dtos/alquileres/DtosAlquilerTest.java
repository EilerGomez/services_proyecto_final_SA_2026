package gt.cunoc.proyecto2.sales.dtos.alquileres;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosAlquilerTest {

    // ----- OrdenAlquilerRespuesta -----

    @Test
    void respuestaDeUnaOrdenPendiente() {
        UUID clienteId = UUID.randomUUID();
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);

        OrdenAlquilerRespuesta respuesta = new OrdenAlquilerRespuesta(orden);

        assertThat(respuesta.ordenId()).isEqualTo(orden.getId());
        assertThat(respuesta.alquilerId()).isEqualTo(orden.getAlquilerId());
        assertThat(respuesta.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(respuesta.clienteNit()).isEqualTo("1234567-8");
        assertThat(respuesta.clienteNombre()).isEqualTo("Colegio San José");
        assertThat(respuesta.clienteCorreo()).isEqualTo("colegio@correo.com");
        assertThat(respuesta.origen()).isEqualTo("Quetzaltenango");
        assertThat(respuesta.destino()).isEqualTo("Antigua Guatemala");
        assertThat(respuesta.monto()).isEqualByComparingTo("2600.00");
        assertThat(respuesta.metodoPago()).isNull();
        assertThat(respuesta.fechaPago()).isNull();
        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(respuesta.motivoRechazo()).isNull();
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void respuestaDeUnaOrdenRechazadaConMotivo() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(UUID.randomUUID());
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.rechazarPagoConCartera("SALDO_INSUFICIENTE");

        OrdenAlquilerRespuesta respuesta = new OrdenAlquilerRespuesta(orden);

        assertThat(respuesta.metodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(respuesta.fechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenAlquiler.RECHAZADA);
        assertThat(respuesta.motivoRechazo()).isEqualTo("SALDO_INSUFICIENTE");
    }

    // ----- FiltroOrdenes -----

    @Test
    void adminSistemaVeTodasSinCambios() {
        FiltroOrdenes original = new FiltroOrdenes(DatosPrueba.GUATE, EstadoOrdenAlquiler.PAGADA);

        assertThat(original.paraUsuario(DatosPrueba.adminSistema())).isSameAs(original);
    }

    @Test
    void cajeroSinSucursalEnElFiltroUsaLaSuya() {
        FiltroOrdenes resultado = new FiltroOrdenes(null, EstadoOrdenAlquiler.PENDIENTE_PAGO)
                .paraUsuario(DatosPrueba.cajero(DatosPrueba.XELA));

        assertThat(resultado.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(resultado.estado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
    }

    @Test
    void adminSucursalConSuPropiaSucursal() {
        FiltroOrdenes resultado = new FiltroOrdenes(DatosPrueba.XELA, null)
                .paraUsuario(DatosPrueba.adminSucursal(DatosPrueba.XELA));

        assertThat(resultado.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(resultado.estado()).isNull();
    }

    @Test
    void otraSucursalEsSucursalAjena() {
        UsuarioActual admin = DatosPrueba.adminSucursal(DatosPrueba.XELA);
        FiltroOrdenes filtro = new FiltroOrdenes(DatosPrueba.GUATE, null);

        VerificarError.lanza(() -> filtro.paraUsuario(admin), "SUCURSAL_AJENA");
    }

    @Test
    void usuarioSinSucursalEsSucursalAjena() {
        UsuarioActual cajero = DatosPrueba.cajero(null);
        FiltroOrdenes filtro = new FiltroOrdenes(null, null);

        VerificarError.lanza(() -> filtro.paraUsuario(cajero), "SUCURSAL_AJENA");
    }
}
