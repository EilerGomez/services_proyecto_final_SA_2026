package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletRechargeRequest;
import gt.cunoc.proyecto2.wallet.dtos.WalletResponse;
import gt.cunoc.proyecto2.wallet.errores.ErrorCartera;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.pagos.ProveedorPagos;
import gt.cunoc.proyecto2.wallet.pagos.ResultadoCobro;
import gt.cunoc.proyecto2.wallet.pagos.SolicitudCobro;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
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
 * Las recargas se cobran con la pasarela configurada ({@link ProveedorPagos}).
 * La cartera de un cliente se crea al registrarse (evento CuentaCreada) o la primera vez que la usa.
 */
@Service
public class WalletServiceImpl implements WalletService {

    /**
     * Sin "hasta" no hay tope: la fecha del movimiento es la fecha de negocio que elige el usuario
     * y puede ser posterior a hoy (antes se cortaba en mañana y esos movimientos no aparecían).
     */
    private static final LocalDate SIN_LIMITE = LocalDate.of(9999, 12, 31);

    private static final String MANUAL = "MANUAL";

    private final CarteraRepository carteras;
    private final MovimientoRepository movimientos;
    private final ProveedorPagos pagos;

    public WalletServiceImpl(CarteraRepository carteras, MovimientoRepository movimientos, ProveedorPagos pagos) {
        this.carteras = carteras;
        this.movimientos = movimientos;
        this.pagos = pagos;
    }

    @Override
    @Transactional
    public boolean abrirCartera(UUID usuarioId) {
        return carteras.crearSiNoExiste(usuarioId) > 0;
    }

    @Override
    @Transactional
    public WalletResponse saldo(UUID usuarioId) {
        carteras.crearSiNoExiste(usuarioId);
        Cartera cartera = carteras.findByUsuarioId(usuarioId).orElseThrow(WalletServiceImpl::sinCartera);
        return new WalletResponse(cartera.getId(), cartera.getSaldo(), cartera.getFechaActualizacion());
    }

    @Override
    @Transactional
    public WalletMovementResponse recargar(UUID usuarioId, WalletRechargeRequest solicitud) {
        BigDecimal monto = solicitud.monto();
        validarMontoRecarga(monto);
        carteras.crearSiNoExiste(usuarioId);

        // 1. Antes de cobrar se revisa que el nuevo saldo sea válido, para no cobrar una recarga que no cabe
        carteras.findByUsuarioId(usuarioId).ifPresent(actual -> nuevoSaldo(actual.getSaldo(), monto));

        // 2. Cobro en la pasarela (antes de bloquear la cartera)
        ResultadoCobro cobro = pagos.cobrar(new SolicitudCobro(
                UUID.randomUUID(), usuarioId, monto, SolicitudCobro.QUETZALES));
        if (!cobro.aprobado()) {
            throw ErrorCartera.PAGO_RECHAZADO.excepcion(
                    cobro.motivo() == null ? ErrorCartera.PAGO_RECHAZADO_SIN_MOTIVO : cobro.motivo());
        }

        // 3. Acreditación con la cartera bloqueada (se vuelve a validar con el saldo ya bloqueado)
        Cartera cartera = carteras.findWithLockByUsuarioId(usuarioId).orElseThrow(WalletServiceImpl::sinCartera);
        LocalDate fecha = solicitud.fechaMovimiento() == null ? LocalDate.now() : solicitud.fechaMovimiento();
        cartera.setSaldo(nuevoSaldo(cartera.getSaldo(), monto));
        carteras.save(cartera);

        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setCarteraId(cartera.getId());
        movimiento.setTipo(MovimientoCartera.RECARGA);
        movimiento.setMonto(monto);
        movimiento.setSaldoResultante(cartera.getSaldo());
        movimiento.setTipoReferencia(MANUAL);
        movimiento.setProveedorPago(pagos.codigo());
        movimiento.setReferenciaExterna(cobro.referenciaExterna());
        movimiento.setFechaMovimiento(fecha);
        movimientos.save(movimiento);
        return aRespuesta(movimiento);
    }

    @Override
    @Transactional
    public Page<WalletMovementResponse> movimientos(UUID usuarioId, LocalDate desde, LocalDate hasta, Pageable paginado) {
        carteras.crearSiNoExiste(usuarioId);
        Cartera cartera = carteras.findByUsuarioId(usuarioId).orElseThrow(WalletServiceImpl::sinCartera);

        LocalDate inicio = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate fin = hasta == null ? SIN_LIMITE : hasta;

        return movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(cartera.getId(), inicio, fin, paginado)
                .map(this::aRespuesta);
    }

    @Override
    @Transactional
    public Optional<MovimientoCartera> intentarDebitar(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaMovimiento) {
        Cartera cartera = carteras.findWithLockByUsuarioId(usuarioId).orElse(null);

        if (cartera == null) {
            return Optional.empty();
        }
        if (cartera.getSaldo().compareTo(monto) < 0) {
            return Optional.empty();
        }

        cartera.setSaldo(nuevoSaldo(cartera.getSaldo(), monto.negate()));
        carteras.save(cartera);
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setCarteraId(cartera.getId());
        movimiento.setTipo(MovimientoCartera.DEBITO);
        movimiento.setMonto(monto);
        movimiento.setSaldoResultante(cartera.getSaldo());
        movimiento.setTipoReferencia(referenciaTipo);
        movimiento.setReferenciaId(referenciaId);
        movimiento.setFechaMovimiento(fechaMovimiento == null ? LocalDate.now() : fechaMovimiento);

        try {
            movimientos.saveAndFlush(movimiento);
        } catch (DataIntegrityViolationException duplicado) {
            return movimientos.findByTipoAndReferenciaId(MovimientoCartera.DEBITO, referenciaId);
        }
        return Optional.of(movimiento);
    }

    @Override
    @Transactional
    public Optional<MovimientoCartera> solicitarReembolso(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, String motivo, LocalDate fechaMovimiento) {
        Cartera cartera = carteras.findWithLockByUsuarioId(usuarioId).orElseThrow(WalletServiceImpl::sinCartera);

        Optional<MovimientoCartera> existente = movimientos.findByTipoAndReferenciaId(MovimientoCartera.REEMBOLSO, referenciaId);

        if (existente.isPresent()) {
            return existente;
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw ErrorCartera.MONTO_INVALIDO.excepcion();
        }

        cartera.setSaldo(nuevoSaldo(cartera.getSaldo(), monto));
        carteras.save(cartera);
        MovimientoCartera movimiento = new MovimientoCartera();
        movimiento.setCarteraId(cartera.getId());
        movimiento.setTipo(MovimientoCartera.REEMBOLSO);
        movimiento.setMonto(monto);
        movimiento.setSaldoResultante(cartera.getSaldo());
        movimiento.setTipoReferencia(referenciaTipo);
        movimiento.setReferenciaId(referenciaId);
        movimiento.setFechaMovimiento(fechaMovimiento == null ? LocalDate.now() : fechaMovimiento);

        try {
            movimientos.saveAndFlush(movimiento);
        } catch (DataIntegrityViolationException duplicado) {
            return movimientos.findByTipoAndReferenciaId(MovimientoCartera.REEMBOLSO, referenciaId);
        }
        return Optional.of(movimiento);
    }

    /** Mayor que cero y con dos decimales como máximo (la columna es NUMERIC(12,2)). */
    private static void validarMontoRecarga(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw ErrorCartera.MONTO_RECARGA_INVALIDO.excepcion();
        }
    }

    /**
     * Saldo que queda al aplicar el cambio (positivo suma, negativo resta).
     * Nunca puede quedar negativo ni pasar del máximo que cabe en la base de datos.
     */
    static BigDecimal nuevoSaldo(BigDecimal saldoActual, BigDecimal cambio) {
        BigDecimal nuevo = saldoActual.add(cambio);
        if (nuevo.signum() < 0) {
            throw ErrorCartera.SALDO_NEGATIVO.excepcion();
        }
        if (nuevo.compareTo(Cartera.SALDO_MAXIMO) > 0) {
            throw ErrorCartera.SALDO_EXCEDE_LIMITE.excepcion(Cartera.SALDO_MAXIMO.toPlainString());
        }
        return nuevo;
    }

    private static NegocioException sinCartera() {
        return ErrorCartera.CARTERA_NO_EXISTE.excepcion();
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
