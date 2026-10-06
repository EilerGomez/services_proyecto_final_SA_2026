package gt.cunoc.proyecto2.identity.services.cartera;


import gt.cunoc.proyecto2.identity.dtos.cartera.WalletMovementResponse;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletRechargeRequest;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletResponse;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


/**
 * Cartera digital: recargas manuales y saga de débito/reembolso con Sales.
 */
public interface WalletService {


    WalletResponse saldo(UUID usuarioId);


    WalletMovementResponse recargar(UUID usuarioId, WalletRechargeRequest request);


    Page<WalletMovementResponse> movimientos(UUID usuarioId, LocalDate desde, LocalDate hasta, Pageable paginado);


    Optional<MovimientoCartera> intentarDebitar(
            UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaMovimiento);


    Optional<MovimientoCartera> solicitarReembolso(
            UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, String motivo,
            LocalDate fechaMovimiento);

}
