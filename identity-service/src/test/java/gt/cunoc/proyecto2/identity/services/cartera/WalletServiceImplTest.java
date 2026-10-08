package gt.cunoc.proyecto2.identity.services.cartera;

import gt.cunoc.proyecto2.identity.dtos.cartera.WalletRechargeRequest;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID REFERENCIA_ID = UUID.randomUUID();
    private static final BigDecimal MONTO = new BigDecimal("150.00");

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;
    @Mock private UsuarioRepository usuarios;

    @InjectMocks
    private WalletServiceImpl servicio;

    @Test
    void recargarSumaAlSaldoConFechaElegida() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("100.00"));
        when(carteras.findWithLockByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));
        ArgumentCaptor<MovimientoCartera> guardado = ArgumentCaptor.forClass(MovimientoCartera.class);

        // Act
        var respuesta = servicio.recargar(USUARIO_ID,
                new WalletRechargeRequest(MONTO, LocalDate.of(2026, 10, 5)));

        // Assert
        assertAll(
                () -> assertEquals(0, new BigDecimal("250.00").compareTo(respuesta.saldoResultante())),
                () -> verify(movimientos).save(guardado.capture()),
                () -> assertEquals("RECARGA", guardado.getValue().getTipo()),
                () -> assertEquals(LocalDate.of(2026, 10, 5), guardado.getValue().getFechaMovimiento()));
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
                () -> verify(movimientos, org.mockito.Mockito.never()).saveAndFlush(any()));
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
    void saldoDevuelveSaldo() {
        // Arrange
        Cartera cartera = cartera(new BigDecimal("350.00"));
        when(carteras.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cartera));

        // Act
        var respuesta = servicio.saldo(USUARIO_ID);

        // Assert
        assertEquals(0, new BigDecimal("350.00").compareTo(respuesta.saldo()));
    }

    @Test
    void saldoSinCarteraDa404() {
        // Arrange
        when(carteras.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.NoEncontradoException.class,
                () -> servicio.saldo(USUARIO_ID));

        // Assert
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
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(movimiento)));

        // Act
        var pagina = servicio.movimientos(USUARIO_ID, null, null,
                org.springframework.data.domain.Pageable.unpaged());

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
