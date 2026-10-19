package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.DatosPrueba;
import gt.cunoc.proyecto2.wallet.pagos.ProveedorPagos;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** WalletServiceImpl.movimientos con fechas explícitas (ramas desde/hasta no nulos). */
@ExtendWith(MockitoExtension.class)
class WalletMovimientosFechasTest {

    @Mock private CarteraRepository carteras;
    @Mock private MovimientoRepository movimientos;
    @Mock private ProveedorPagos pagos;

    @InjectMocks private WalletServiceImpl servicio;

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void movimientosConFechasUsaRangoDado() {
        Cartera cartera = DatosPrueba.cartera(usuarioId, "0.00");
        when(carteras.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartera));
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 30);
        when(movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                eq(cartera.getId()), eq(desde), eq(hasta), any(Pageable.class)))
                .thenReturn(Page.empty());

        var pagina = servicio.movimientos(usuarioId, desde, hasta, Pageable.unpaged());

        assertTrue(pagina.isEmpty());
        verify(movimientos).findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                eq(cartera.getId()), eq(desde), eq(hasta), any(Pageable.class));
    }
}
