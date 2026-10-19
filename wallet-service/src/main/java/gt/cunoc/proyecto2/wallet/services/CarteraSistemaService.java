package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.wallet.dtos.CarteraSistemaResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.errores.ErrorCartera;
import gt.cunoc.proyecto2.wallet.models.Cartera;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Billetera del sistema: no es de ningún usuario.
 * <ul>
 *   <li>COBRO: al debitar la cartera del cliente por una compra en línea, el monto entra aquí.</li>
 *   <li>DEVOLUCION: al reembolsar esa compra, el monto sale de aquí.</li>
 * </ul>
 * Se registran en la MISMA transacción que el débito o el reembolso del cliente (la saga con Sales no
 * cambia). Bloqueo: primero la cartera del cliente y después la del sistema, siempre en ese orden.
 * No hay retiros: cada devolución corresponde a un cobro previo, así que el saldo nunca queda negativo.
 */
@Service
public class CarteraSistemaService {

    /**
     * Sin "hasta" no hay tope: la fecha del movimiento es la fecha de negocio que elige el usuario
     * y puede ser posterior a hoy (antes se cortaba en mañana y esos movimientos no aparecían).
     */
    private static final LocalDate SIN_LIMITE = LocalDate.of(9999, 12, 31);

    private static final Logger log = LoggerFactory.getLogger(CarteraSistemaService.class);
    private static final Set<String> TIPOS = Set.of(MovimientoCartera.COBRO, MovimientoCartera.DEVOLUCION);

    private final CarteraRepository carteras;
    private final MovimientoRepository movimientos;

    public CarteraSistemaService(CarteraRepository carteras, MovimientoRepository movimientos) {
        this.carteras = carteras;
        this.movimientos = movimientos;
    }

    /** Pago de una compra en línea: entra a la billetera del sistema. Idempotente por referencia. */
    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoCartera registrarCobro(BigDecimal monto, String referenciaTipo, UUID referenciaId,
                                            LocalDate fecha) {
        Optional<MovimientoCartera> previo = movimientos.findByTipoAndReferenciaId(MovimientoCartera.COBRO, referenciaId);
        if (previo.isPresent()) {
            return previo.get();
        }
        Cartera sistema = sistema(true);
        sistema.acreditar(monto);
        carteras.save(sistema);
        return movimientos.save(MovimientoCartera.cobro(sistema, monto, referenciaTipo, referenciaId, fecha));
    }

    /**
     * Reembolso de una compra: sale de la billetera del sistema. Idempotente por referencia.
     * Si esa compra nunca entró a la billetera (se hizo antes de que existiera), no se descuenta nada.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<MovimientoCartera> registrarDevolucion(BigDecimal monto, String referenciaTipo, UUID referenciaId,
                                                           LocalDate fecha) {
        Optional<MovimientoCartera> previa =
                movimientos.findByTipoAndReferenciaId(MovimientoCartera.DEVOLUCION, referenciaId);
        if (previa.isPresent()) {
            return previa;
        }
        if (movimientos.findByTipoAndReferenciaId(MovimientoCartera.COBRO, referenciaId).isEmpty()) {
            log.warn("Reembolso de {} {} sin cobro en la billetera del sistema: no se descuenta", referenciaTipo,
                    referenciaId);
            return Optional.empty();
        }
        Cartera sistema = sistema(true);
        // No debería quedar negativa: no hay retiros y cada devolución tiene su cobro. Si pasa, se revierte.
        sistema.debitar(monto);
        carteras.save(sistema);
        return Optional.of(movimientos.save(
                MovimientoCartera.devolucion(sistema, monto, referenciaTipo, referenciaId, fecha)));
    }

    /** W4: saldo y totales (solo ADMIN_SISTEMA). */
    @Transactional
    public CarteraSistemaResponse saldo() {
        Cartera sistema = sistema(false);
        return new CarteraSistemaResponse(sistema.getId(), sistema.getSaldo(),
                movimientos.sumarPorTipo(sistema.getId(), MovimientoCartera.COBRO),
                movimientos.sumarPorTipo(sistema.getId(), MovimientoCartera.DEVOLUCION),
                sistema.getFechaActualizacion());
    }

    /** W5: movimientos de la billetera del sistema, del más reciente al más antiguo. tipo: COBRO o DEVOLUCION. */
    @Transactional
    public Page<WalletMovementResponse> movimientos(String tipo, LocalDate desde, LocalDate hasta, Pageable paginado) {
        if (tipo != null && !tipo.isBlank() && !TIPOS.contains(tipo)) {
            throw ErrorCartera.TIPO_INVALIDO.excepcion();
        }
        Cartera sistema = sistema(false);
        LocalDate inicio = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate fin = hasta == null ? SIN_LIMITE : hasta;
        Page<MovimientoCartera> pagina = tipo == null || tipo.isBlank()
                ? movimientos.findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                        sistema.getId(), inicio, fin, paginado)
                : movimientos.findByCarteraIdAndTipoAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
                        sistema.getId(), tipo, inicio, fin, paginado);
        return pagina.map(WalletMovementResponse::de);
    }

    private Cartera sistema(boolean bloquear) {
        carteras.crearSistemaSiNoExiste();
        Optional<Cartera> sistema = bloquear
                ? carteras.findWithLockByTipo(Cartera.SISTEMA)
                : carteras.findByTipo(Cartera.SISTEMA);
        return sistema.orElseThrow(() -> new IllegalStateException("No existe la billetera del sistema"));
    }
}
