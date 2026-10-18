package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.wallet.dtos.WalletRechargeRequest;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.pagos.ProveedorPagos;
import gt.cunoc.proyecto2.wallet.pagos.ResultadoCobro;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas borde de WalletServiceImpl no cubiertas por WalletServiceImplTest. */
@ExtendWith(MockitoExtension.class)
class WalletRamasTest {

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;
    @Mock private ProveedorPagos pagos;

    @InjectMocks private WalletServiceImpl servicio;

    private final UUID usuarioId = UUID.randomUUID();

    private Cartera cartera(String saldo) {
        Cartera cartera = new Cartera();
        cartera.setId(UUID.randomUUID());
        cartera.setUsuarioId(usuarioId);
        cartera.setSaldo(new BigDecimal(saldo));
        return cartera;
    }

    @Test
    void recargarSinFechaUsaHoy() {
        when(pagos.cobrar(any())).thenReturn(ResultadoCobro.aprobado("SIM-1"));
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("100.00")));
        ArgumentCaptor<MovimientoCartera> guardado = ArgumentCaptor.forClass(MovimientoCartera.class);

        var respuesta = servicio.recargar(usuarioId, new WalletRechargeRequest(new BigDecimal("50.00"), null));

        verify(movimientos).save(guardado.capture());
        assertEquals(0, new BigDecimal("150.00").compareTo(respuesta.saldoResultante()));
        assertEquals(LocalDate.now(), guardado.getValue().getFechaMovimiento());
    }

    @Test
    void recargarCobradaSinCarteraDa404() {
        when(pagos.cobrar(any())).thenReturn(ResultadoCobro.aprobado("SIM-1"));
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        NoEncontradoException error = assertThrows(NoEncontradoException.class, () -> servicio.recargar(usuarioId,
                new WalletRechargeRequest(new BigDecimal("50.00"), LocalDate.now())));
        assertEquals("CARTERA_NO_EXISTE", error.getCodigo());
    }

    @Test
    void movimientosSinCarteraDa404YSinFechasUsaDefectos() {
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.movimientos(usuarioId, null, null,
                Pageable.unpaged()));

        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("0.00")));
        when(movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                any(), any(), any(), any())).thenReturn(Page.empty());
        var pagina = servicio.movimientos(usuarioId, null, null, Pageable.unpaged());
        assertTrue(pagina.isEmpty());
    }

    @Test
    void debitarSinCarteraDevuelveVacio() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertTrue(servicio.intentarDebitar(usuarioId, BigDecimal.TEN, "VENTA",
                UUID.randomUUID(), LocalDate.now()).isEmpty());
        verify(movimientos, never()).saveAndFlush(any());
    }

    @Test
    void debitarSinFechaUsaHoy() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("100.00")));

        var resultado = servicio.intentarDebitar(usuarioId, BigDecimal.TEN, "ALQUILER", UUID.randomUUID(), null);

        assertTrue(resultado.isPresent());
        assertEquals(LocalDate.now(), resultado.get().getFechaMovimiento());
    }

    @Test
    void debitarDuplicadoConcurrenteDevuelveElExistente() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("100.00")));
        UUID referenciaId = UUID.randomUUID();
        MovimientoCartera existente = new MovimientoCartera();
        when(movimientos.saveAndFlush(any(MovimientoCartera.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));
        when(movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId))
                .thenReturn(Optional.of(existente));

        var resultado = servicio.intentarDebitar(usuarioId, BigDecimal.TEN, "VENTA",
                referenciaId, null);

        assertTrue(resultado.isPresent());
        assertEquals(existente, resultado.get());
    }

    @Test
    void reembolsoSinCarteraMontoNuloOCero() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.solicitarReembolso(usuarioId,
                BigDecimal.TEN, "VENTA", UUID.randomUUID(), "m", LocalDate.now()));

        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("0.00")));
        when(movimientos.findByTipoAndReferenciaId(any(), any())).thenReturn(Optional.empty());
        ReglaNegocioException nulo = assertThrows(ReglaNegocioException.class,
                () -> servicio.solicitarReembolso(usuarioId, null, "VENTA", UUID.randomUUID(),
                        "m", LocalDate.now()));
        assertEquals("MONTO_INVALIDO", nulo.getCodigo());
        ReglaNegocioException cero = assertThrows(ReglaNegocioException.class,
                () -> servicio.solicitarReembolso(usuarioId, BigDecimal.ZERO, "VENTA",
                        UUID.randomUUID(), "m", LocalDate.now()));
        assertEquals("MONTO_INVALIDO", cero.getCodigo());
    }

    @Test
    void reembolsoOkSinFechaUsaHoyYLuegoDevuelveElExistente() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("0.00")));
        UUID referenciaId = UUID.randomUUID();
        when(movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId))
                .thenReturn(Optional.empty());
        when(movimientos.saveAndFlush(any(MovimientoCartera.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = servicio.solicitarReembolso(usuarioId, new BigDecimal("25.00"), "VENTA",
                referenciaId, "m", null);

        assertTrue(resultado.isPresent());
        assertEquals(LocalDate.now(), resultado.get().getFechaMovimiento());

        MovimientoCartera existente = new MovimientoCartera();
        when(movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId))
                .thenReturn(Optional.of(existente));

        assertEquals(existente, servicio.solicitarReembolso(usuarioId, new BigDecimal("25.00"),
                "VENTA", referenciaId, "m", LocalDate.now()).get());
    }

    @Test
    void reembolsoDuplicadoAlGuardarDevuelveExistente() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("0.00")));
        UUID referenciaId = UUID.randomUUID();
        MovimientoCartera existente = new MovimientoCartera();
        when(movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId))
                .thenReturn(Optional.empty(), Optional.of(existente));
        when(movimientos.saveAndFlush(any(MovimientoCartera.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertEquals(existente, servicio.solicitarReembolso(usuarioId, new BigDecimal("25.00"),
                "VENTA", referenciaId, "m", LocalDate.now()).get());
    }
}
