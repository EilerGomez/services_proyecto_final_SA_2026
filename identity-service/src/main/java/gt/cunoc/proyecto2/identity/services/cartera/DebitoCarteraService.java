package gt.cunoc.proyecto2.identity.services.cartera;

import gt.cunoc.proyecto2.identity.messaging.publisher.CarteraEventPublisher;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Saga de pago: debita y responde CarteraDebitada o DebitoCarteraRechazado.
 *
 * <p>El rechazo es un resultado normal: se publica y la transacción se confirma.
 */
@Service
public class DebitoCarteraService {

    private final UsuarioRepository usuarios;
    private final CarteraRepository carteras;
    private final MovimientoRepository movimientos;
    private final WalletService servicio;
    private final CarteraEventPublisher publicador;

    public DebitoCarteraService(UsuarioRepository usuarios, CarteraRepository carteras, MovimientoRepository movimientos, WalletService servicio, CarteraEventPublisher publicador) {
        this.usuarios = usuarios;
        this.carteras = carteras;
        this.movimientos = movimientos;
        this.servicio = servicio;
        this.publicador = publicador;
    }

    @Transactional
    public void procesar(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        if (usuario == null || !"ACTIVO".equals(usuario.getEstado())) {
            rechazar(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, "USUARIO_INACTIVO");
            return;
        }

        if (carteras.findByUsuarioId(usuarioId).isEmpty()) {
            rechazar(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, "CARTERA_INEXISTENTE");
            return;
        }

        Optional<MovimientoCartera> previo = movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId);

        if (previo.isPresent()) {
            publicarDebitada(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, previo.get());
            return;
        }

        Optional<MovimientoCartera> debito = servicio.intentarDebitar(
                usuarioId, monto, referenciaTipo,
                referenciaId, fechaNegocio);

        if (debito.isPresent()) {
            publicarDebitada(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, debito.get());
        } else {
            rechazar(usuarioId, monto, referenciaTipo, referenciaId, fechaNegocio, "SALDO_INSUFICIENTE");
        }
    }

    private void publicarDebitada(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio, MovimientoCartera movimiento) {
        publicador.publicarCarteraDebitada(
                movimiento.getId(), referenciaTipo, referenciaId,
                usuarioId, monto,
                movimiento.getSaldoResultante(), movimiento.getFechaMovimiento(),
                fechaNegocio);
    }

    private void rechazar(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio, String motivo) {
        publicador.publicarDebitoCarteraRechazado(
                referenciaTipo, referenciaId, usuarioId,
                monto, motivo,
                fechaNegocio);
    }
}
