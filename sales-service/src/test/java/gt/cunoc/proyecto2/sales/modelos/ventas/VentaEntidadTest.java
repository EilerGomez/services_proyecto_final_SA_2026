package gt.cunoc.proyecto2.sales.modelos.ventas;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VentaEntidadTest {

    private final VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
    private final UUID clienteId = UUID.randomUUID();

    // ----- Fábricas -----

    @Test
    void registrarEnMostradorQuedaConfirmadaEnEfectivo() {
        UUID cajeroId = UUID.randomUUID();
        ClienteVenta cliente = new ClienteVenta(null, "1234567-8", "Ana López", "ana@correo.com");

        VentaEntidad venta = VentaEntidad.registrarEnMostrador(viaje, cliente, 3, FECHA_PAGO, cajeroId);

        assertThat(venta.getId()).isNotNull();
        assertThat(venta.getViajeId()).isEqualTo(viaje.getViajeId());
        assertThat(venta.getSucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(venta.getCanal()).isEqualTo(CanalVenta.PRESENCIAL);
        assertThat(venta.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(venta.getCajeroUsuarioId()).isEqualTo(cajeroId);
        assertThat(venta.getClienteUsuarioId()).isNull();
        assertThat(venta.getClienteNit()).isEqualTo("1234567-8");
        assertThat(venta.getClienteNombre()).isEqualTo("Ana López");
        assertThat(venta.getClienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(venta.getMontoTotal()).isEqualByComparingTo("225.00");
        assertThat(venta.getCantidadBoletos()).isEqualTo(3);
        assertThat(venta.getFechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.getMotivoRechazo()).isNull();
        assertThat(venta.getMotivoCancelacion()).isNull();
        assertThat(venta.getFechaCancelacion()).isNull();
        assertThat(venta.getVersion()).isNull();
        assertThat(venta.pagadaConCartera()).isFalse();
        assertThat(venta.estaPendienteDePago()).isFalse();
    }

    @Test
    void registrarEnMostradorAConsumidorFinal() {
        VentaEntidad venta = VentaEntidad.registrarEnMostrador(viaje, new ClienteVenta(null, null, null, null), 1,
                FECHA_PAGO, UUID.randomUUID());

        assertThat(venta.getClienteNit()).isEqualTo("CF");
        assertThat(venta.getClienteNombre()).isEqualTo("Consumidor final");
        assertThat(venta.getClienteCorreo()).isNull();
        assertThat(venta.getMontoTotal()).isEqualByComparingTo("75.00");
    }

    @Test
    void registrarEnLineaQuedaPendienteDePagoConCartera() {
        VentaEntidad venta = VentaEntidad.registrarEnLinea(viaje, DatosPrueba.clienteConCuenta(clienteId), 2,
                FECHA_PAGO);

        assertThat(venta.getId()).isNotNull();
        assertThat(venta.getViajeId()).isEqualTo(viaje.getViajeId());
        assertThat(venta.getSucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(venta.getCanal()).isEqualTo(CanalVenta.EN_LINEA);
        assertThat(venta.getMetodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(venta.getCajeroUsuarioId()).isNull();
        assertThat(venta.getClienteUsuarioId()).isEqualTo(clienteId);
        assertThat(venta.getClienteNit()).isEqualTo("1234567-8");
        assertThat(venta.getClienteNombre()).isEqualTo("Ana López");
        assertThat(venta.getClienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(venta.getMontoTotal()).isEqualByComparingTo("150.00");
        assertThat(venta.getCantidadBoletos()).isEqualTo(2);
        assertThat(venta.getFechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.PENDIENTE_PAGO);
        assertThat(venta.getVersion()).isNull();
        assertThat(venta.pagadaConCartera()).isTrue();
        assertThat(venta.estaPendienteDePago()).isTrue();
    }

    @Test
    void cadaVentaTieneSuPropioId() {
        VentaEntidad una = DatosPrueba.ventaMostrador(viaje, 1);
        VentaEntidad otra = DatosPrueba.ventaMostrador(viaje, 1);

        assertThat(una.getId()).isNotEqualTo(otra.getId());
    }

    // ----- cancelar (S5) -----

    @Test
    void cancelarUnaConfirmada() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 2);
        LocalDate fecha = FECHA_PAGO.plusDays(2);

        venta.cancelar("El cliente ya no viaja", fecha);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(venta.getMotivoCancelacion()).isEqualTo("El cliente ya no viaja");
        assertThat(venta.getFechaCancelacion()).isEqualTo(fecha);
    }

    @Test
    void cancelarElMismoDiaDelPagoEsValido() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);

        venta.cancelar("Error del cajero", FECHA_PAGO);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(venta.getFechaCancelacion()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void cancelarConFechaAnteriorAlPagoEsFechaInvalida() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);
        LocalDate anterior = FECHA_PAGO.minusDays(1);

        assertThatThrownBy(() -> venta.cancelar("Motivo", anterior))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorVenta.FECHA_CANCELACION_INVALIDA.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(400);
                    assertThat(e.getMessage()).isEqualTo(
                            "La fecha de cancelación (2026-10-31) no puede ser anterior a la fecha de pago (2026-11-01)");
                });
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.getMotivoCancelacion()).isNull();
        assertThat(venta.getFechaCancelacion()).isNull();
    }

    @Test
    void cancelarUnaPendienteEsEstadoInvalido() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);

        assertThatThrownBy(() -> venta.cancelar("Motivo", FECHA_PAGO))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorVenta.ESTADO_INVALIDO.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(422);
                    assertThat(e.getMessage())
                            .isEqualTo("La venta está PENDIENTE_PAGO; esta acción solo aplica a ventas CONFIRMADA");
                });
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.PENDIENTE_PAGO);
    }

    @Test
    void cancelarDosVecesEsEstadoInvalidoAntesQueLaFecha() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);
        venta.cancelar("Primera", FECHA_PAGO);

        VerificarError.lanza(() -> venta.cancelar("Segunda", FECHA_PAGO.minusDays(5)), ErrorVenta.ESTADO_INVALIDO);
        assertThat(venta.getMotivoCancelacion()).isEqualTo("Primera");
    }

    // ----- cancelarPorViajeEliminado -----

    @Test
    void viajeEliminadoCancelaConLaFechaDeHoy() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);
        LocalDate hoy = FECHA_PAGO.plusDays(3);

        venta.cancelarPorViajeEliminado("VIAJE_ELIMINADO", hoy);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(venta.getMotivoCancelacion()).isEqualTo("VIAJE_ELIMINADO");
        assertThat(venta.getFechaCancelacion()).isEqualTo(hoy);
    }

    @Test
    void viajeEliminadoConPagoEnFechaFuturaCancelaConLaFechaDePago() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);
        LocalDate hoy = FECHA_PAGO.minusDays(10);

        venta.cancelarPorViajeEliminado("VIAJE_ELIMINADO", hoy);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(venta.getFechaCancelacion()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void viajeEliminadoNoCancelaUnaPendiente() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);

        VerificarError.lanza(() -> venta.cancelarPorViajeEliminado("VIAJE_ELIMINADO", FECHA_PAGO),
                ErrorVenta.ESTADO_INVALIDO);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.PENDIENTE_PAGO);
    }

    // ----- confirmarPago y rechazar (saga) -----

    @Test
    void confirmarPagoDeUnaPendiente() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 2);

        venta.confirmarPago();

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.estaPendienteDePago()).isFalse();
    }

    @Test
    void confirmarPagoDeUnaConfirmadaEsEstadoInvalido() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);

        assertThatThrownBy(venta::confirmarPago)
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorVenta.ESTADO_INVALIDO.codigo());
                    assertThat(e.getMessage())
                            .isEqualTo("La venta está CONFIRMADA; esta acción solo aplica a ventas PENDIENTE_PAGO");
                });
    }

    @Test
    void rechazarUnaPendienteGuardaElMotivo() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);

        venta.rechazar("SALDO_INSUFICIENTE");

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(venta.getMotivoRechazo()).isEqualTo("SALDO_INSUFICIENTE");
    }

    @Test
    void rechazarUnaConfirmadaEsEstadoInvalido() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);

        VerificarError.lanza(() -> venta.rechazar("TIEMPO_AGOTADO"), ErrorVenta.ESTADO_INVALIDO);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.getMotivoRechazo()).isNull();
    }

    // ----- marcarReembolsada y reembolsarEnEfectivo -----

    @Test
    void unaCanceladaSeMarcaReembolsada() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);
        venta.cancelar("Motivo", FECHA_PAGO);

        assertThat(venta.puedeMarcarseReembolsada()).isTrue();
        venta.marcarReembolsada(FECHA_PAGO.plusDays(1));

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REEMBOLSADA);
        assertThat(venta.getFechaReembolso()).isEqualTo(FECHA_PAGO.plusDays(1));
        assertThat(venta.estaReembolsada()).isTrue();
        assertThat(venta.puedeMarcarseReembolsada()).isFalse();
    }

    @Test
    void elReembolsoNuncaQuedaAntesDelPago() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);
        venta.cancelar("Motivo", FECHA_PAGO);

        venta.marcarReembolsada(FECHA_PAGO.minusDays(3));

        assertThat(venta.getFechaReembolso()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void unaRechazadaSeMarcaReembolsada() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);
        venta.rechazar("TIEMPO_AGOTADO");

        assertThat(venta.puedeMarcarseReembolsada()).isTrue();
        venta.marcarReembolsada(FECHA_PAGO);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REEMBOLSADA);
    }

    @Test
    void unaConfirmadaNoSeMarcaReembolsada() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);

        assertThat(venta.puedeMarcarseReembolsada()).isFalse();
        assertThatThrownBy(() -> venta.marcarReembolsada(FECHA_PAGO))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorVenta.ESTADO_INVALIDO.codigo());
                    assertThat(e.getMessage()).isEqualTo(
                            "La venta está CONFIRMADA; esta acción solo aplica a ventas CANCELADA o RECHAZADA");
                });
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
    }

    @Test
    void unaPendienteNoSeMarcaReembolsadaYUnaYaReembolsadaTampoco() {
        VentaEntidad pendiente = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);
        VentaEntidad reembolsada = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);
        reembolsada.rechazar("SALDO_INSUFICIENTE");
        reembolsada.marcarReembolsada(FECHA_PAGO);

        assertThat(pendiente.puedeMarcarseReembolsada()).isFalse();
        VerificarError.lanza(() -> pendiente.marcarReembolsada(FECHA_PAGO), ErrorVenta.ESTADO_INVALIDO);
        VerificarError.lanza(() -> reembolsada.marcarReembolsada(FECHA_PAGO), ErrorVenta.VENTA_YA_REEMBOLSADA);
    }

    @Test
    void elEfectivoDeUnaCanceladaSeDevuelveYQuedaQuienLoEntrego() {
        UUID cajeroId = UUID.randomUUID();
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 2);
        venta.cancelarPorViajeEliminado("Viaje eliminado · Xela → Guate", FECHA_PAGO.plusDays(1));

        venta.reembolsarEnEfectivo(FECHA_PAGO.plusDays(2), cajeroId);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REEMBOLSADA);
        assertThat(venta.getFechaReembolso()).isEqualTo(FECHA_PAGO.plusDays(2));
        assertThat(venta.getReembolsadoPor()).isEqualTo(cajeroId);
    }

    @Test
    void loPagadoConCarteraNoSeDevuelveEnEfectivo() {
        UUID cajeroId = UUID.randomUUID();
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);
        venta.cancelar("Motivo", FECHA_PAGO);

        VerificarError.lanza(() -> venta.reembolsarEnEfectivo(FECHA_PAGO, cajeroId),
                ErrorVenta.REEMBOLSO_NO_EN_EFECTIVO);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
    }

    @Test
    void unaConfirmadaNoSeReembolsaEnEfectivo() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);

        VerificarError.lanza(() -> venta.reembolsarEnEfectivo(FECHA_PAGO, UUID.randomUUID()),
                ErrorVenta.ESTADO_INVALIDO);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
    }

    // ----- Código del boleto impreso -----

    @Test
    void elCodigoSonLosDocePrimerosCaracteresDelIdEnMayusculas() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);

        String esperado = venta.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        assertThat(venta.codigo()).isEqualTo(esperado).hasSize(12).isUpperCase();
    }

    @Test
    void normalizarCodigoAceptaGuionesMinusculasYElIdCompleto() {
        UUID id = UUID.fromString("a1b2c3d4-e5f6-4789-8abc-def012345678");

        assertThat(VentaEntidad.codigoDe(id)).isEqualTo("A1B2C3D4E5F6");
        assertThat(VentaEntidad.normalizarCodigo("a1b2c3d4e5f6")).isEqualTo("A1B2C3D4E5F6");
        assertThat(VentaEntidad.normalizarCodigo("A1B2-C3D4-E5F6")).isEqualTo("A1B2C3D4E5F6");
        assertThat(VentaEntidad.normalizarCodigo(id.toString())).isEqualTo("A1B2C3D4E5F6");
    }

    @Test
    void normalizarCodigoDevuelveNuloSiNoLleganDoceCaracteres() {
        assertThat(VentaEntidad.normalizarCodigo(null)).isNull();
        assertThat(VentaEntidad.normalizarCodigo("   ")).isNull();
        assertThat(VentaEntidad.normalizarCodigo("A1B2-C3D4")).isNull();
        assertThat(VentaEntidad.normalizarCodigo("ZZZZ-ZZZZ-ZZZZ")).isNull();
        assertThat(VentaEntidad.codigoDe(null)).isNull();
    }

    // ----- Consultas -----

    @Test
    void precioPorBoletoEsElMontoEntreLaCantidad() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 4);

        assertThat(venta.getMontoTotal()).isEqualByComparingTo("300.00");
        assertThat(venta.precioPorBoleto()).isEqualByComparingTo("75.00");
        assertThat(venta.precioPorBoleto().scale()).isEqualTo(2);
    }

    @Test
    void precioPorBoletoRedondeaADosDecimales() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 3);
        ReflectionTestUtils.setField(venta, "montoTotal", new BigDecimal("100.00"));

        assertThat(venta.precioPorBoleto()).isEqualByComparingTo("33.33");
    }

    @Test
    void fechaNoAnteriorAlPago() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);

        assertThat(venta.fechaNoAnteriorAlPago(FECHA_PAGO.minusDays(1))).isEqualTo(FECHA_PAGO);
        assertThat(venta.fechaNoAnteriorAlPago(FECHA_PAGO)).isEqualTo(FECHA_PAGO);
        assertThat(venta.fechaNoAnteriorAlPago(FECHA_PAGO.plusDays(1))).isEqualTo(FECHA_PAGO.plusDays(1));
    }

    @Test
    void esDelClienteSoloSiTieneCuentaYCoincide() {
        VentaEntidad enLinea = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);
        VentaEntidad mostrador = DatosPrueba.ventaMostrador(viaje, 1);

        assertThat(enLinea.esDelCliente(clienteId)).isTrue();
        assertThat(enLinea.esDelCliente(UUID.randomUUID())).isFalse();
        assertThat(enLinea.esDelCliente(null)).isFalse();
        assertThat(mostrador.esDelCliente(UUID.randomUUID())).isFalse();
        assertThat(mostrador.esDelCliente(null)).isFalse();
    }

    @Test
    void pagadaConCarteraSegunElMetodo() {
        assertThat(DatosPrueba.ventaEnLinea(viaje, clienteId, 1).pagadaConCartera()).isTrue();
        assertThat(DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1).pagadaConCartera()).isTrue();
        assertThat(DatosPrueba.ventaMostrador(viaje, 1).pagadaConCartera()).isFalse();
    }

    @Test
    void estaPendienteDePagoSoloEnPendientePago() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 1);
        assertThat(venta.estaPendienteDePago()).isTrue();

        venta.rechazar("SALDO_INSUFICIENTE");

        assertThat(venta.estaPendienteDePago()).isFalse();
    }
}
