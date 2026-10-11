package gt.cunoc.proyecto2.sales.modelos.alquileres;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.errores.alquileres.ErrorOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrdenAlquilerEntidadTest {

    private final UUID clienteId = UUID.randomUUID();

    // ----- registrar -----

    @Test
    void registrarCopiaLaCotizacionYQuedaPendienteSinMetodo() {
        UUID alquilerId = UUID.randomUUID();
        ClienteVenta cliente = new ClienteVenta(clienteId, "1234567-8", "Colegio San José", "colegio@correo.com");

        OrdenAlquilerEntidad orden = OrdenAlquilerEntidad.registrar(alquilerId, DatosPrueba.XELA, cliente,
                "Quetzaltenango", "Antigua Guatemala", new BigDecimal("2600.00"));

        assertThat(orden.getId()).isNotNull();
        assertThat(orden.getAlquilerId()).isEqualTo(alquilerId);
        assertThat(orden.getSucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(orden.getClienteUsuarioId()).isEqualTo(clienteId);
        assertThat(orden.getClienteNit()).isEqualTo("1234567-8");
        assertThat(orden.getClienteNombre()).isEqualTo("Colegio San José");
        assertThat(orden.getClienteCorreo()).isEqualTo("colegio@correo.com");
        assertThat(orden.getOrigen()).isEqualTo("Quetzaltenango");
        assertThat(orden.getDestino()).isEqualTo("Antigua Guatemala");
        assertThat(orden.getMonto()).isEqualByComparingTo("2600.00");
        assertThat(orden.getMetodoPago()).isNull();
        assertThat(orden.getFechaPago()).isNull();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(orden.getMotivoRechazo()).isNull();
        assertThat(orden.getFechaCreacion()).isNull();
        assertThat(orden.getVersion()).isNull();
        assertThat(orden.cobroConCarteraEnCurso()).isFalse();
        assertThat(orden.pagadaConCartera()).isFalse();
    }

    @Test
    void registrarAlquilerDeMostradorAConsumidorFinal() {
        OrdenAlquilerEntidad orden = OrdenAlquilerEntidad.registrar(UUID.randomUUID(), DatosPrueba.XELA,
                new ClienteVenta(null, null, null, null), "Quetzaltenango", "Antigua Guatemala",
                new BigDecimal("2600.00"));

        assertThat(orden.getClienteUsuarioId()).isNull();
        assertThat(orden.getClienteNit()).isEqualTo("CF");
        assertThat(orden.getClienteNombre()).isEqualTo("Consumidor final");
        assertThat(orden.getClienteCorreo()).isNull();
    }

    // ----- solicitarPagoConCartera (S12) -----

    @Test
    void solicitarPagoConCarteraQuedaPendienteConCarteraYFecha() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);

        orden.solicitarPagoConCartera(FECHA_PAGO);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(orden.getFechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(orden.getMotivoRechazo()).isNull();
        assertThat(orden.cobroConCarteraEnCurso()).isTrue();
        assertThat(orden.pagadaConCartera()).isFalse();
    }

    @Test
    void solicitarPagoConCarteraSinCuentaEsAlquilerSinCuenta() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(null);

        assertThatThrownBy(() -> orden.solicitarPagoConCartera(FECHA_PAGO))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorOrdenAlquiler.ALQUILER_SIN_CUENTA.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(422);
                });
        assertThat(orden.getMetodoPago()).isNull();
        assertThat(orden.getFechaPago()).isNull();
    }

    @Test
    void sinCuentaSeRevisaAntesQueElEstado() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(null);
        orden.pagarEnEfectivo(FECHA_PAGO);

        VerificarError.lanza(() -> orden.solicitarPagoConCartera(FECHA_PAGO), ErrorOrdenAlquiler.ALQUILER_SIN_CUENTA);
    }

    @Test
    void solicitarDosVecesEsPagoEnProceso() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);

        assertThatThrownBy(() -> orden.solicitarPagoConCartera(FECHA_PAGO.plusDays(1)))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorOrdenAlquiler.PAGO_EN_PROCESO.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(409);
                });
        assertThat(orden.getFechaPago()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void solicitarPagoDeUnaPagadaEsEstadoInvalido() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.pagarEnEfectivo(FECHA_PAGO);

        assertThatThrownBy(() -> orden.solicitarPagoConCartera(FECHA_PAGO))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorOrdenAlquiler.ESTADO_INVALIDO.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(422);
                    assertThat(e.getMessage())
                            .isEqualTo("La orden está PAGADA; solo se paga si está PENDIENTE_PAGO o RECHAZADA");
                });
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
    }

    @Test
    void solicitarPagoDeUnaReembolsadaEsEstadoInvalido() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        ReflectionTestUtils.setField(orden, "estado", EstadoOrdenAlquiler.REEMBOLSADA);

        VerificarError.lanza(() -> orden.solicitarPagoConCartera(FECHA_PAGO), ErrorOrdenAlquiler.ESTADO_INVALIDO);
    }

    @Test
    void reintentarElPagoConCarteraDesdeRechazada() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.rechazarPagoConCartera("SALDO_INSUFICIENTE");
        LocalDate nuevaFecha = FECHA_PAGO.plusDays(2);

        orden.solicitarPagoConCartera(nuevaFecha);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(orden.getFechaPago()).isEqualTo(nuevaFecha);
        assertThat(orden.getMotivoRechazo()).isNull();
        assertThat(orden.cobroConCarteraEnCurso()).isTrue();
    }

    // ----- pagarEnEfectivo (S13) -----

    @Test
    void pagarEnEfectivoUnaPendiente() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(null);

        orden.pagarEnEfectivo(FECHA_PAGO);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(orden.getFechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(orden.getMotivoRechazo()).isNull();
        assertThat(orden.pagadaConCartera()).isFalse();
        assertThat(orden.cobroConCarteraEnCurso()).isFalse();
    }

    @Test
    void pagarEnEfectivoConCobroConCarteraEnCurso() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        LocalDate fechaEfectivo = FECHA_PAGO.plusDays(1);

        orden.pagarEnEfectivo(fechaEfectivo);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(orden.getFechaPago()).isEqualTo(fechaEfectivo);
        assertThat(orden.cobroConCarteraEnCurso()).isFalse();
    }

    @Test
    void pagarEnEfectivoUnaRechazadaLimpiaElMotivo() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.rechazarPagoConCartera("SALDO_INSUFICIENTE");

        orden.pagarEnEfectivo(FECHA_PAGO);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(orden.getMotivoRechazo()).isNull();
    }
}
