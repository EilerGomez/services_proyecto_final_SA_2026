package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.wallet.dtos.CarteraSistemaResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteraSistemaServiceTest {

    private static final UUID REFERENCIA = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    private static final Pageable PAGINA = PageRequest.of(0, 20);

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;

    private CarteraSistemaService servicio;
    private Cartera sistema;

    @BeforeEach
    void preparar() {
        servicio = new CarteraSistemaService(carteras, movimientos);
        sistema = new Cartera();
        sistema.setId(UUID.randomUUID());
        sistema.setTipo(Cartera.SISTEMA);
        sistema.setSaldo(new BigDecimal("100.00"));
    }

    private void sistemaBloqueada() {
        when(carteras.findWithLockByTipo(Cartera.SISTEMA)).thenReturn(Optional.of(sistema));
        when(movimientos.save(any(MovimientoCartera.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static MovimientoCartera movimiento(String tipo) {
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setId(UUID.randomUUID());
        movimiento.setTipo(tipo);
        movimiento.setMonto(new BigDecimal("40.00"));
        movimiento.setSaldoResultante(new BigDecimal("60.00"));
        movimiento.setTipoReferencia("VENTA");
        movimiento.setReferenciaId(REFERENCIA);
        movimiento.setFechaMovimiento(FECHA);
        return movimiento;
    }

    @Test
    void cobroSumaAlSaldoYGuardaElMovimiento() {
        sistemaBloqueada();

        MovimientoCartera cobro = servicio.registrarCobro(new BigDecimal("40.00"), "VENTA", REFERENCIA, FECHA);

        assertThat(sistema.getSaldo()).isEqualByComparingTo("140.00");
        assertThat(cobro.getTipo()).isEqualTo(MovimientoCartera.COBRO);
        assertThat(cobro.getCarteraId()).isEqualTo(sistema.getId());
        assertThat(cobro.getMonto()).isEqualByComparingTo("40.00");
        assertThat(cobro.getSaldoResultante()).isEqualByComparingTo("140.00");
        assertThat(cobro.getTipoReferencia()).isEqualTo("VENTA");
        assertThat(cobro.getReferenciaId()).isEqualTo(REFERENCIA);
        assertThat(cobro.getFechaMovimiento()).isEqualTo(FECHA);
        verify(carteras).crearSistemaSiNoExiste();
        verify(carteras).save(sistema);
    }

    @Test
    void cobroRepetidoDevuelveElMismoSinTocarElSaldo() {
        MovimientoCartera previo = movimiento(MovimientoCartera.COBRO);
        when(movimientos.findByTipoAndReferenciaId(MovimientoCartera.COBRO, REFERENCIA)).thenReturn(Optional.of(previo));

        assertThat(servicio.registrarCobro(new BigDecimal("40.00"), "VENTA", REFERENCIA, FECHA)).isSameAs(previo);
        verifyNoInteractions(carteras);
    }

    @Test
    void cobroSinFechaUsaHoy() {
        sistemaBloqueada();

        assertThat(servicio.registrarCobro(BigDecimal.ONE, "ALQUILER", REFERENCIA, null).getFechaMovimiento())
                .isEqualTo(LocalDate.now());
    }

    @Test
    void devolucionRestaDelSaldo() {
        when(movimientos.findByTipoAndReferenciaId(MovimientoCartera.DEVOLUCION, REFERENCIA)).thenReturn(Optional.empty());
        when(movimientos.findByTipoAndReferenciaId(MovimientoCartera.COBRO, REFERENCIA))
                .thenReturn(Optional.of(movimiento(MovimientoCartera.COBRO)));
        sistemaBloqueada();

        Optional<MovimientoCartera> devolucion =
                servicio.registrarDevolucion(new BigDecimal("40.00"), "VENTA", REFERENCIA, FECHA);

        assertThat(devolucion).get().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(MovimientoCartera.DEVOLUCION);
            assertThat(m.getMonto()).isEqualByComparingTo("40.00");
            assertThat(m.getSaldoResultante()).isEqualByComparingTo("60.00");
        });
        assertThat(sistema.getSaldo()).isEqualByComparingTo("60.00");
    }

    @Test
    void devolucionRepetidaDevuelveLaMisma() {
        MovimientoCartera previa = movimiento(MovimientoCartera.DEVOLUCION);
        when(movimientos.findByTipoAndReferenciaId(MovimientoCartera.DEVOLUCION, REFERENCIA))
                .thenReturn(Optional.of(previa));

        assertThat(servicio.registrarDevolucion(new BigDecimal("40.00"), "VENTA", REFERENCIA, FECHA)).containsSame(previa);
        verifyNoInteractions(carteras);
    }

    @Test
    void devolucionDeUnaCompraQueNuncaEntroNoDescuentaNada() {
        assertThat(servicio.registrarDevolucion(new BigDecimal("40.00"), "VENTA", REFERENCIA, FECHA)).isEmpty();
        verifyNoInteractions(carteras);
        verify(movimientos, never()).save(any());
    }

    @Test
    void devolucionMayorQueElSaldoSeRechazaSinGuardar() {
        when(movimientos.findByTipoAndReferenciaId(MovimientoCartera.DEVOLUCION, REFERENCIA)).thenReturn(Optional.empty());
        when(movimientos.findByTipoAndReferenciaId(MovimientoCartera.COBRO, REFERENCIA))
                .thenReturn(Optional.of(movimiento(MovimientoCartera.COBRO)));
        when(carteras.findWithLockByTipo(Cartera.SISTEMA)).thenReturn(Optional.of(sistema));

        assertThatThrownBy(() -> servicio.registrarDevolucion(new BigDecimal("100.01"), "VENTA", REFERENCIA, FECHA))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SALDO_SISTEMA_INSUFICIENTE"));
        assertThat(sistema.getSaldo()).isEqualByComparingTo("100.00");
        verify(carteras, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void sinBilleteraDelSistemaEsUnErrorDeConfiguracion() {
        assertThatThrownBy(() -> servicio.registrarCobro(BigDecimal.ONE, "VENTA", REFERENCIA, FECHA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void saldoConTotales() {
        when(carteras.findByTipo(Cartera.SISTEMA)).thenReturn(Optional.of(sistema));
        when(movimientos.sumarPorTipo(sistema.getId(), MovimientoCartera.COBRO)).thenReturn(new BigDecimal("150.00"));
        when(movimientos.sumarPorTipo(sistema.getId(), MovimientoCartera.DEVOLUCION)).thenReturn(new BigDecimal("50.00"));

        CarteraSistemaResponse respuesta = servicio.saldo();

        assertThat(respuesta.carteraId()).isEqualTo(sistema.getId());
        assertThat(respuesta.saldo()).isEqualByComparingTo("100.00");
        assertThat(respuesta.totalCobrado()).isEqualByComparingTo("150.00");
        assertThat(respuesta.totalDevuelto()).isEqualByComparingTo("50.00");
    }

    @Test
    void movimientosSinFiltrosUsanTodoElRango() {
        when(carteras.findByTipo(Cartera.SISTEMA)).thenReturn(Optional.of(sistema));
        Page<MovimientoCartera> pagina = new PageImpl<>(List.of(movimiento(MovimientoCartera.COBRO)));
        when(movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                eq(sistema.getId()), eq(LocalDate.of(2000, 1, 1)), eq(LocalDate.of(9999, 12, 31)), eq(PAGINA)))
                .thenReturn(pagina);

        List<WalletMovementResponse> lista = servicio.movimientos(" ", null, null, PAGINA).getContent();

        assertThat(lista).singleElement().satisfies(m -> {
            assertThat(m.tipo()).isEqualTo(MovimientoCartera.COBRO);
            assertThat(m.referenciaId()).isEqualTo(REFERENCIA);
        });
    }

    @Test
    void movimientosPorTipoYFechas() {
        when(carteras.findByTipo(Cartera.SISTEMA)).thenReturn(Optional.of(sistema));
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        ArgumentCaptor<String> tipo = ArgumentCaptor.forClass(String.class);
        when(movimientos.findByCarteraIdAndTipoAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                eq(sistema.getId()), tipo.capture(), eq(desde), eq(hasta), eq(PAGINA)))
                .thenReturn(Page.empty());

        assertThat(servicio.movimientos(MovimientoCartera.DEVOLUCION, desde, hasta, PAGINA)).isEmpty();
        assertThat(tipo.getValue()).isEqualTo(MovimientoCartera.DEVOLUCION);
    }

    @Test
    void movimientosConTipoInvalido() {
        assertThatThrownBy(() -> servicio.movimientos("RECARGA", null, null, PAGINA))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("TIPO_INVALIDO"));
        verifyNoInteractions(carteras, movimientos);
    }
}
