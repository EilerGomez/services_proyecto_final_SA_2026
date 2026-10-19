package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.wallet.dtos.WalletRechargeRequest;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.DatosPrueba;
import gt.cunoc.proyecto2.wallet.pagos.ProveedorPagos;
import gt.cunoc.proyecto2.wallet.pagos.ResultadoCobro;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Validaciones de la recarga: monto válido y saldo resultante dentro del máximo, antes y después de cobrar. */
@ExtendWith(MockitoExtension.class)
class RecargaSaldoTest {

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;
    @Mock private ProveedorPagos pagos;

    @InjectMocks private WalletServiceImpl servicio;

    private final UUID usuarioId = UUID.randomUUID();

    private Cartera cartera(String saldo) {
        return DatosPrueba.cartera(usuarioId, saldo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-10.00", "10.555"})
    void recargaConMontoInvalidoNoSeCobra(String monto) {
        var error = assertThrows(SolicitudInvalidaException.class, () -> servicio.recargar(usuarioId,
                new WalletRechargeRequest(new BigDecimal(monto), LocalDate.now())));

        assertEquals("MONTO_RECARGA_INVALIDO", error.getCodigo());
        verify(pagos, never()).cobrar(any());
        verify(carteras, never()).save(any());
    }

    @Test
    void recargaSinMontoNoSeCobra() {
        var error = assertThrows(SolicitudInvalidaException.class, () -> servicio.recargar(usuarioId,
                new WalletRechargeRequest(null, LocalDate.now())));

        assertEquals("MONTO_RECARGA_INVALIDO", error.getCodigo());
        verify(pagos, never()).cobrar(any());
    }

    @Test
    void recargaQueSuperaElSaldoMaximoNoSeCobra() {
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("9999999999.00")));

        var error = assertThrows(ReglaNegocioException.class, () -> servicio.recargar(usuarioId,
                new WalletRechargeRequest(new BigDecimal("1.00"), LocalDate.now())));

        assertEquals("SALDO_EXCEDE_LIMITE", error.getCodigo());
        verify(pagos, never()).cobrar(any());
        verify(carteras, never()).save(any());
    }

    @Test
    void recargaHastaElSaldoMaximoSeAcepta() {
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("9999999998.99")));
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("9999999998.99")));
        when(pagos.cobrar(any())).thenReturn(ResultadoCobro.aprobado("SIM-1"));

        var respuesta = servicio.recargar(usuarioId,
                new WalletRechargeRequest(new BigDecimal("1.00"), LocalDate.now()));

        assertEquals(0, Cartera.SALDO_MAXIMO.compareTo(respuesta.saldoResultante()));
    }

    @Test
    void siElSaldoCambioMientrasSeCobrabaSeValidaDeNuevoConLaCarteraBloqueada() {
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("100.00")));
        when(pagos.cobrar(any())).thenReturn(ResultadoCobro.aprobado("SIM-1"));
        when(carteras.findWithLockByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera("9999999999.99")));

        var error = assertThrows(ReglaNegocioException.class, () -> servicio.recargar(usuarioId,
                new WalletRechargeRequest(new BigDecimal("1.00"), LocalDate.now())));

        assertEquals("SALDO_EXCEDE_LIMITE", error.getCodigo());
        verify(carteras, never()).save(any());
    }
}
