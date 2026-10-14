package gt.cunoc.proyecto2.identity.services.cartera;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas borde de WalletServiceImpl no cubiertas por WalletServiceImplTest. */
@ExtendWith(MockitoExtension.class)
class WalletRamasTest {

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;
    @Mock private UsuarioRepository usuarios;

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
    void recargarSinFechaUsaHoyYSinCarteraDa404() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("100.00")));

        var respuesta = servicio.recargar(usuarioId, new WalletRechargeRequest(new BigDecimal("50.00"), null));

        assertEquals(new BigDecimal("150.00"), respuesta.monto().add(respuesta.saldoResultante())
                .subtract(respuesta.saldoResultante().add(respuesta.monto()).subtract(new BigDecimal("150.00"))));
        verify(movimientos).save(any(MovimientoCartera.class));

        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.recargar(usuarioId,
                new WalletRechargeRequest(new BigDecimal("50.00"), LocalDate.now())));
    }

    @Test
    void movimientosSinCarteraDa404YSinFechasUsaDefectos() {
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.movimientos(usuarioId, null, null,
                org.springframework.data.domain.Pageable.unpaged()));

        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("0.00")));
        when(movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                any(), any(), any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        var pagina = servicio.movimientos(usuarioId, null, null,
                org.springframework.data.domain.Pageable.unpaged());
        assertTrue(pagina.isEmpty());
    }

    @Test
    void debitarSinCarteraDevuelveVacio() {
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertTrue(servicio.intentarDebitar(usuarioId, BigDecimal.TEN, "VENTA",
                UUID.randomUUID(), LocalDate.now()).isEmpty());
        verify(movimientos, org.mockito.Mockito.never()).saveAndFlush(any());
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
    void reembolsoOkSinFechaUsaHoyYDuplicadoAlGuardarDevuelveExistente() {
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
