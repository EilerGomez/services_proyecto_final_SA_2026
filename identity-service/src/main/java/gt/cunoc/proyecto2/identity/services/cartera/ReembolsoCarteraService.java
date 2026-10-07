package gt.cunoc.proyecto2.identity.services.cartera;

import gt.cunoc.proyecto2.identity.messaging.publisher.CarteraEventPublisher;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Compensación: acredita el reembolso si existe el débito y publica CarteraReembolsada.
 */
@Service
public class ReembolsoCarteraService {

    private static final Logger log = LoggerFactory.getLogger(ReembolsoCarteraService.class);
    private final MovimientoRepository movimientos;
    private final WalletService servicio;
    private final CarteraEventPublisher publicador;

    public ReembolsoCarteraService(MovimientoRepository movimientos, WalletService servicio, CarteraEventPublisher publicador) {
        this.movimientos = movimientos;
        this.servicio = servicio;
        this.publicador = publicador;
    }

    @Transactional
    public void procesar(UUID usuarioId, BigDecimal monto, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio, String motivo) {
        Optional<MovimientoCartera> debito =
                movimientos.findByTipoAndReferenciaId("DEBITO", referenciaId);

        if (debito.isEmpty()) {
            log.warn("Reembolso sin débito previo, referencia {}", referenciaId);
            return;
        }

        Optional<MovimientoCartera> previo =
                movimientos.findByTipoAndReferenciaId("REEMBOLSO", referenciaId);

        if (previo.isPresent()) {
            publicar(usuarioId, referenciaTipo, referenciaId, fechaNegocio, previo.get());
            return;
        }

        Optional<MovimientoCartera> reembolso = servicio.solicitarReembolso(
                usuarioId, monto, referenciaTipo,
                referenciaId, motivo, fechaNegocio);
        reembolso.ifPresent(movimiento -> publicar(usuarioId, referenciaTipo, referenciaId, fechaNegocio, movimiento));
    }

    private void publicar(UUID usuarioId, String referenciaTipo, UUID referenciaId, LocalDate fechaNegocio, MovimientoCartera movimiento) {
        publicador.publicarCarteraReembolsada(
                movimiento.getId(), referenciaTipo, referenciaId,
                usuarioId, movimiento.getMonto(),
                movimiento.getSaldoResultante(), movimiento.getFechaMovimiento(),
                fechaNegocio);
    }
}
