package gt.cunoc.proyecto2.wallet.services;

import gt.cunoc.proyecto2.wallet.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.wallet.errores.MotivoRechazoDebito;
import gt.cunoc.proyecto2.wallet.messaging.publisher.CarteraEventPublisher;
import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import gt.cunoc.proyecto2.wallet.repositories.CarteraRepository;
import gt.cunoc.proyecto2.wallet.repositories.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Saga de pago: debita y responde CarteraDebitada o DebitoCarteraRechazado.
 * El monto debitado entra a la billetera del sistema (COBRO) en la misma transacción.
 *
 * <p>El rechazo es un resultado normal: se publica y la transacción se confirma.
 * El estado de la cuenta lo da Identity (R4c); si Identity no responde, el evento se reintenta.
 */
@Service
public class DebitoCarteraService {

    private final ClienteIdentity identity;
    private final CarteraRepository carteras;
    private final MovimientoRepository movimientos;
    private final WalletService servicio;
    private final CarteraEventPublisher publicador;
    private final CarteraSistemaService sistema;

    public DebitoCarteraService(ClienteIdentity identity, CarteraRepository carteras, MovimientoRepository movimientos,
                                WalletService servicio, CarteraEventPublisher publicador,
                                CarteraSistemaService sistema) {
        this.identity = identity;
        this.carteras = carteras;
        this.movimientos = movimientos;
        this.servicio = servicio;
        this.publicador = publicador;
        this.sistema = sistema;
    }

    @Transactional
    public void procesar(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio) {
        if (!identity.cuentaActiva(usuarioId)) {//REST A identity si la cuenta esta activa
            rechazar(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, MotivoRechazoDebito.USUARIO_INACTIVO);
            return;
        }

        if (carteras.findByUsuarioId(usuarioId).isEmpty()) {
            rechazar(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, MotivoRechazoDebito.CARTERA_INEXISTENTE);
            return;
        }

        Optional<MovimientoCartera> previo = movimientos.findByTipoAndReferenciaId(MovimientoCartera.DEBITO, referenciaId);

        if (previo.isPresent()) {
            publicarDebitada(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, previo.get());
            return;
        }

        Optional<MovimientoCartera> debito = servicio.intentarDebitar(
                usuarioId, monto, referenciaTipo,
                referenciaId, fechaNegocio);

        if (debito.isPresent()) {
            // El pago entra a la billetera del sistema en esta misma transacción
            sistema.registrarCobro(monto, referenciaTipo, referenciaId, debito.get().getFechaMovimiento());
            publicarDebitada(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, debito.get());
        } else {
            rechazar(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, MotivoRechazoDebito.SALDO_INSUFICIENTE);
        }
    }

    private void publicarDebitada(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio, MovimientoCartera movimiento) {
        publicador.publicarCarteraDebitada(
                movimiento.getId(), referenciaTipo, referenciaId,
                usuarioId, monto,
                movimiento.getSaldoResultante(), movimiento.getFechaMovimiento(),
                fechaNegocio);
    }

    private void rechazar(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio, MotivoRechazoDebito motivo) {
        publicador.publicarDebitoCarteraRechazado(
                referenciaTipo, referenciaId, usuarioId,
                monto, motivo.name(),
                fechaNegocio);
    }
}
