package gt.cunoc.proyecto2.identity.services.cartera;


import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletMovementResponse;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletRechargeRequest;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletResponse;
import gt.cunoc.proyecto2.identity.models.Cartera;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


/**
 * Débitos con bloqueo pesimista e idempotencia por UNIQUE(tipo, referencia_id).
 */
@Service
public class WalletServiceImpl implements WalletService {

    private final CarteraRepository carteras;
    private final MovimientoRepository movimientos;
    private final UsuarioRepository usuarios;

    public WalletServiceImpl(
            CarteraRepository carteras,
            MovimientoRepository movimientos,
            UsuarioRepository usuarios) {
        this.carteras = carteras;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse saldo(UUID usuarioId) {
        Cartera cartera = carteras.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("CARTERA_NO_EXISTE", "El usuario no tiene cartera"));
        return new WalletResponse(cartera.getId(), cartera.getSaldo(), cartera.getFechaActualizacion());
    }

    @Override
    @Transactional
    public WalletMovementResponse recargar(UUID usuarioId, WalletRechargeRequest solicitud) {
        Cartera cartera = carteras.findWithLockByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("CARTERA_NO_EXISTE", "El usuario no tiene cartera"));

        LocalDate fecha = solicitud.fechaMovimiento() == null ? LocalDate.now() : solicitud.fechaMovimiento();
        cartera.setSaldo(cartera.getSaldo().add(solicitud.monto()));
        carteras.save(cartera);

        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setCarteraId(cartera.getId());
        movimiento.setTipo("RECARGA");
        movimiento.setMonto(solicitud.monto());
        movimiento.setSaldoResultante(cartera.getSaldo());
        movimiento.setTipoReferencia("MANUAL");
        movimiento.setFechaMovimiento(fecha);
        movimientos.save(movimiento);
        return aRespuesta(movimiento);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletMovementResponse> movimientos(UUID usuarioId, LocalDate desde, LocalDate hasta, Pageable paginado) {
        Cartera cartera = carteras.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("CARTERA_NO_EXISTE", "El usuario no tiene cartera"));
        LocalDate inicio = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate fin = hasta == null ? LocalDate.now().plusDays(1) : hasta;
        return movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(cartera.getId(), inicio, fin, paginado)
                .map(this::aRespuesta);
    }

    @Override
    @Transactional
    public Optional<MovimientoCartera> intentarDebitar(
            UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaMovimiento) {
        Cartera cartera = carteras.findWithLockByUsuarioId(usuarioId).orElse(null);
        if (cartera == null) {
            return Optional.empty();
        }
        if (cartera.getSaldo().compareTo(monto) < 0) {
            return Optional.empty();
        }
        cartera.setSaldo(cartera.getSaldo().subtract(monto));
        carteras.save(cartera);
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setCarteraId(cartera.getId());
        movimiento.setTipo("DEBITO");
        movimiento.setMonto(monto);
        movimiento.setSaldoResultante(cartera.getSaldo());
        movimiento.setTipoReferencia(referenciaTipo);
        movimiento.setReferenciaId(referenciaId);
        movimiento.setFechaMovimiento(fechaMovimiento == null ? LocalDate.now() : fechaMovimiento);
        try {
            movimientos.saveAndFlush(movimiento);
        } catch (DataIntegrityViolationException duplicado) {
            return movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId);
        }
        return Optional.of(movimiento);
    }

    @Override
    @Transactional
    public Optional<MovimientoCartera> solicitarReembolso(
            UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, String motivo,
            LocalDate fechaMovimiento) {
        Cartera cartera = carteras.findWithLockByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("CARTERA_NO_EXISTE", "El usuario no tiene cartera"));
        Optional<MovimientoCartera> existente = movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId);
        if (existente.isPresent()) {
            return existente;
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("MONTO_INVALIDO", "El monto del reembolso debe ser mayor que cero");
        }
        cartera.setSaldo(cartera.getSaldo().add(monto));
        carteras.save(cartera);
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setCarteraId(cartera.getId());
        movimiento.setTipo("REEMBOLSO");
        movimiento.setMonto(monto);
        movimiento.setSaldoResultante(cartera.getSaldo());
        movimiento.setTipoReferencia(referenciaTipo);
        movimiento.setReferenciaId(referenciaId);
        movimiento.setFechaMovimiento(fechaMovimiento == null ? LocalDate.now() : fechaMovimiento);
        try {
            movimientos.saveAndFlush(movimiento);
        } catch (DataIntegrityViolationException duplicado) {
            return movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId);
        }
        return Optional.of(movimiento);
    }

    private WalletMovementResponse aRespuesta(MovimientoCartera movimiento) {
        return new WalletMovementResponse(
                movimiento.getId(),
                movimiento.getTipo(),
                movimiento.getMonto(),
                movimiento.getSaldoResultante(),
                movimiento.getTipoReferencia(),
                movimiento.getReferenciaId(),
                movimiento.getFechaMovimiento());
    }
}
