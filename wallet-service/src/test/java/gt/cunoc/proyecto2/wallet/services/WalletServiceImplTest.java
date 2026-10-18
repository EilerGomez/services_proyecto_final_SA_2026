package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.wallet.dtos.WalletRechargeRequest;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.pagos.ProveedorPagos;
import gt.cunoc.proyecto2.wallet.pagos.ResultadoCobro;
import gt.cunoc.proyecto2.wallet.pagos.SolicitudCobro;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID REFERENCIA_ID = UUID.randomUUID();
    private static final BigDecimal MONTO = new BigDecimal("150.00");

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;
    @Mock private ProveedorPagos pagos;

    @InjectMocks
    private WalletServiceImpl servicio;

    @Test
    void abrirCarteraIndicaSiLaCreo() {
        when(carteras.crearSiNoExiste(USUARIO_ID)).thenReturn(1, 0);

        assertTrue(servicio.abrirCartera(USUARIO_ID));
        assertFalse(servicio.abrirCartera(USUARIO_ID));
    }

    @Test
    void recargarCobraEnLaPasarelaYSumaAlSaldoConFechaElegida() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("100.00"));
        when(carteras.findWithLockByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));
        when(pagos.cobrar(any())).thenReturn(ResultadoCobro.aprobado("SIM-123"));
        when(pagos.codigo()).thenReturn("SIMULADO");
        ArgumentCaptor<MovimientoCartera> guardado = ArgumentCaptor.forClass(MovimientoCartera.class);
        ArgumentCaptor<SolicitudCobro> cobro = ArgumentCaptor.forClass(SolicitudCobro.class);

        // Act
        var respuesta = servicio.recargar(USUARIO_ID,
                new WalletRechargeRequest(MONTO, LocalDate.of(2026, 10, 5)));

        // Assert
        verify(carteras).crearSiNoExiste(USUARIO_ID);
        verify(pagos).cobrar(cobro.capture());
        verify(movimientos).save(guardado.capture());
        assertAll(
                () -> assertEquals(0, new BigDecimal("250.00").compareTo(respuesta.saldoResultante())),
                () -> assertEquals(USUARIO_ID, cobro.getValue().usuarioId()),
                () -> assertEquals(0, MONTO.compareTo(cobro.getValue().monto())),
                () -> assertEquals(SolicitudCobro.QUETZALES, cobro.getValue().moneda()),
                () -> assertEquals("RECARGA", guardado.getValue().getTipo()),
                () -> assertEquals("MANUAL", guardado.getValue().getTipoReferencia()),
                () -> assertEquals("SIMULADO", guardado.getValue().getProveedorPago()),
                () -> assertEquals("SIM-123", guardado.getValue().getReferenciaExterna()),
                () -> assertEquals(LocalDate.of(2026, 10, 5), guardado.getValue().getFechaMovimiento()));
    }

    @Test
    void recargaRechazadaPorLaPasarelaNoTocaLaCartera() {
        when(pagos.cobrar(any())).thenReturn(ResultadoCobro.rechazado("Tarjeta rechazada"));

        var error = assertThrows(ReglaNegocioException.class, () -> servicio.recargar(USUARIO_ID,
                new WalletRechargeRequest(MONTO, null)));

        assertEquals("PAGO_RECHAZADO", error.getCodigo());
        assertEquals("Tarjeta rechazada", error.getMessage());
        verify(carteras, never()).findWithLockByUsuarioId(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void recargaRechazadaSinMotivoUsaMensajeGenerico() {
        when(pagos.cobrar(any())).thenReturn(new ResultadoCobro(false, null, null));

        var error = assertThrows(ReglaNegocioException.class, () -> servicio.recargar(USUARIO_ID,
                new WalletRechargeRequest(MONTO, null)));

        assertEquals("La pasarela de pagos rechazó el cobro", error.getMessage());
    }

    @Test
    void debitarConSaldoRestaUnaVez() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("500.00"));
        when(carteras.findWithLockByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));

        // Act
        var resultado = servicio.intentarDebitar(USUARIO_ID, MONTO, "VENTA", REFERENCIA_ID, LocalDate.now());

        // Assert
        assertAll(
                () -> assertTrue(resultado.isPresent()),
                () -> assertEquals(0, new BigDecimal("350.00").compareTo(cartera.getSaldo())),
                () -> verify(movimientos).saveAndFlush(any(MovimientoCartera.class)));
    }

    @Test
    void debitarSinSaldoNoTocaLaBase() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("10.00"));
        when(carteras.findWithLockByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));

        // Act
        var resultado = servicio.intentarDebitar(USUARIO_ID, MONTO, "VENTA", REFERENCIA_ID, LocalDate.now());

        // Assert
        assertAll(
                () -> assertTrue(resultado.isEmpty()),
                () -> assertEquals(0, new BigDecimal("10.00").compareTo(cartera.getSaldo())),
                () -> verify(movimientos, never()).saveAndFlush(any()));
    }

    @Test
    void reembolsoDuplicadoDevuelveElExistente() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("100.00"));
        MovimientoCartera previo = new MovimientoCartera();
        previo.setMonto(new BigDecimal("150.00"));
        when(carteras.findWithLockByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));
        when(movimientos.findByTipoAndReferenciaId("REEMBOLSO", REFERENCIA_ID))
                .thenReturn(Optional.of(previo));

        // Act
        var resultado = servicio.solicitarReembolso(
                USUARIO_ID, MONTO, "VENTA", REFERENCIA_ID, "CANCELACION", LocalDate.now());

        // Assert
        assertAll(
                () -> assertTrue(resultado.isPresent()),
                () -> assertEquals(previo, resultado.get()),
                () -> assertEquals(0, new BigDecimal("100.00").compareTo(cartera.getSaldo())));
    }

    @Test
    void saldoAbreLaCarteraSiNoExisteYDevuelveSaldo() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("350.00"));
        when(carteras.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));

        // Act
        var respuesta = servicio.saldo(USUARIO_ID);

        // Assert
        verify(carteras).crearSiNoExiste(USUARIO_ID);
        assertEquals(0, new BigDecimal("350.00").compareTo(respuesta.saldo()));
    }

    @Test
    void saldoSinCarteraDa404() {
        when(carteras.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        var error = assertThrows(NoEncontradoException.class, () -> servicio.saldo(USUARIO_ID));

        assertEquals("CARTERA_NO_EXISTE", error.getCodigo());
    }

    @Test
    void movimientosPaginaMapeada() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("100.00"));
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setId(UUID.randomUUID());
        movimiento.setTipo("RECARGA");
        movimiento.setMonto(new BigDecimal("100.00"));
        movimiento.setSaldoResultante(new BigDecimal("100.00"));
        movimiento.setTipoReferencia("MANUAL");
        movimiento.setFechaMovimiento(LocalDate.of(2026, 10, 5));
        when(carteras.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));
        when(movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(movimiento)));

        // Act
        var pagina = servicio.movimientos(USUARIO_ID, null, null, Pageable.unpaged());

        // Assert
        assertAll(
                () -> assertEquals(1, pagina.getTotalElements()),
                () -> assertEquals("RECARGA", pagina.getContent().get(0).tipo()));
    }

    private Cartera cartera(BigDecimal saldo) {
        Cartera cartera = new Cartera();
        cartera.setUsuarioId(USUARIO_ID);
        cartera.setSaldo(saldo);
        return cartera;
    }
}
