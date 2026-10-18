package gt.cunoc.proyecto2.sales.eventos.publicados.alquileres;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlquilerPagadoTest {

    @Test
    void alquilerPagadoConCartera() {
        UUID clienteId = UUID.randomUUID();
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.confirmarPagoConCartera();

        AlquilerPagado evento = AlquilerPagado.desde(orden);

        assertThat(evento.ordenId()).isEqualTo(orden.getId());
        assertThat(evento.alquilerId()).isEqualTo(orden.getAlquilerId());
        assertThat(evento.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(evento.clienteUsuarioId()).isEqualTo(clienteId);
        assertThat(evento.clienteNit()).isEqualTo("1234567-8");
        assertThat(evento.clienteNombre()).isEqualTo("Colegio San José");
        assertThat(evento.clienteCorreo()).isEqualTo("colegio@correo.com");
        assertThat(evento.origen()).isEqualTo("Quetzaltenango");
        assertThat(evento.destino()).isEqualTo("Antigua Guatemala");
        assertThat(evento.fechaSalida()).isEqualTo(DatosPrueba.SALIDA);
        assertThat(evento.fechaRetorno()).isEqualTo(DatosPrueba.RETORNO);
        assertThat(evento.monto()).isEqualByComparingTo("2600.00");
        assertThat(evento.montoTotal()).isEqualByComparingTo("2600.00");
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(evento.fechaPago()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void alquilerDeMostradorPagadoEnEfectivo() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(null);
        orden.pagarEnEfectivo(FECHA_PAGO.plusDays(1));

        AlquilerPagado evento = AlquilerPagado.desde(orden);

        assertThat(evento.clienteUsuarioId()).isNull();
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(evento.fechaPago()).isEqualTo(FECHA_PAGO.plusDays(1));
        assertThat(evento.montoTotal()).isEqualByComparingTo(evento.monto());
    }
}
