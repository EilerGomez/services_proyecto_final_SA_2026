package gt.cunoc.proyecto2.wallet.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Los métodos de fábrica toman de la cartera su id y el saldo que quedó. */
class MovimientoCarteraTest {

    private static final BigDecimal MONTO = new BigDecimal("40.00");
    private static final UUID REFERENCIA = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    @Test
    void recargaGuardaLaPasarelaYEsManual() {
        Cartera cartera = DatosPrueba.cartera("140.00");

        MovimientoCartera recarga = MovimientoCartera.recarga(cartera, MONTO, "SIMULADO", "SIM-1", FECHA);

        assertThat(recarga.getId()).isNull();
        assertThat(recarga.getCarteraId()).isEqualTo(cartera.getId());
        assertThat(recarga.getTipo()).isEqualTo(MovimientoCartera.RECARGA);
        assertThat(recarga.getMonto()).isEqualByComparingTo("40.00");
        assertThat(recarga.getSaldoResultante()).isEqualByComparingTo("140.00");
        assertThat(recarga.getTipoReferencia()).isEqualTo(MovimientoCartera.MANUAL);
        assertThat(recarga.getReferenciaId()).isNull();
        assertThat(recarga.getProveedorPago()).isEqualTo("SIMULADO");
        assertThat(recarga.getReferenciaExterna()).isEqualTo("SIM-1");
        assertThat(recarga.getFechaMovimiento()).isEqualTo(FECHA);
    }

    @Test
    void cadaFabricaPoneSuTipoYLaReferencia() {
        Cartera cartera = DatosPrueba.cartera("60.00");

        assertThat(MovimientoCartera.debito(cartera, MONTO, "VENTA", REFERENCIA, FECHA).getTipo())
                .isEqualTo(MovimientoCartera.DEBITO);
        assertThat(MovimientoCartera.reembolso(cartera, MONTO, "VENTA", REFERENCIA, FECHA).getTipo())
                .isEqualTo(MovimientoCartera.REEMBOLSO);
        assertThat(MovimientoCartera.cobro(cartera, MONTO, "ALQUILER", REFERENCIA, FECHA).getTipo())
                .isEqualTo(MovimientoCartera.COBRO);
        MovimientoCartera devolucion = MovimientoCartera.devolucion(cartera, MONTO, "ALQUILER", REFERENCIA, FECHA);
        assertThat(devolucion.getTipo()).isEqualTo(MovimientoCartera.DEVOLUCION);
        assertThat(devolucion.getTipoReferencia()).isEqualTo("ALQUILER");
        assertThat(devolucion.getReferenciaId()).isEqualTo(REFERENCIA);
        assertThat(devolucion.getSaldoResultante()).isEqualByComparingTo("60.00");
        assertThat(devolucion.getProveedorPago()).isNull();
    }

    @Test
    void sinFechaUsaHoy() {
        assertThat(MovimientoCartera.debito(DatosPrueba.cartera("0.00"), MONTO, "VENTA", REFERENCIA, null)
                .getFechaMovimiento()).isEqualTo(LocalDate.now());
    }
}
