package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletRechargeRequest;
import gt.cunoc.proyecto2.wallet.dtos.WalletResponse;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Cartera digital: recargas por la pasarela de pagos y saga de débito/reembolso con Sales.
 */
public interface WalletService {

    /** Crea la cartera en cero si el usuario no tiene. @return true si la creó. */
    boolean abrirCartera(UUID usuarioId);

    WalletResponse saldo(UUID usuarioId);

    WalletMovementResponse recargar(UUID usuarioId, WalletRechargeRequest request);

    Page<WalletMovementResponse> movimientos(UUID usuarioId, LocalDate desde, LocalDate hasta, Pageable paginado);

    Optional<MovimientoCartera> intentarDebitar(
            UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaMovimiento);

    Optional<MovimientoCartera> solicitarReembolso(
            UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, String motivo,
            LocalDate fechaMovimiento);
}
