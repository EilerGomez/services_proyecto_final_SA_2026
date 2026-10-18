package gt.cunoc.proyecto2.wallet.messaging.publisher;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.wallet.evento.payload.DebitoRechazadoPayload;
import gt.cunoc.proyecto2.wallet.evento.payload.MovimientoCarteraPayload;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Respuestas de la saga en wallet.eventos (outbox). Mismos nombres y payloads que publicaba Identity:
 * Sales solo cambió el exchange al que se suscribe.
 */
@Component
public class CarteraEventPublisher {

    public static final String AGREGADO = "Cartera";

    private final PublicadorEventos publicador;

    public CarteraEventPublisher(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    public void publicarCarteraDebitada(
            UUID movimientoId,
            String referenciaTipo,
            UUID referenciaId,
            UUID usuarioId,
            BigDecimal monto,
            BigDecimal saldoResultante,
            LocalDate fechaMovimiento,
            LocalDate fechaNegocio) {

        publicador.publicar(AGREGADO, usuarioId, "CarteraDebitada",
                new MovimientoCarteraPayload(movimientoId, referenciaTipo, referenciaId, usuarioId, monto, saldoResultante, fechaMovimiento), fechaNegocio);
    }

    public void publicarCarteraReembolsada(
            UUID movimientoId,
            String referenciaTipo,
            UUID referenciaId,
            UUID usuarioId,
            BigDecimal monto,
            BigDecimal saldoResultante,
            LocalDate fechaMovimiento,
            LocalDate fechaNegocio) {

        publicador.publicar(AGREGADO, usuarioId, "CarteraReembolsada",
                new MovimientoCarteraPayload(movimientoId, referenciaTipo, referenciaId, usuarioId, monto, saldoResultante, fechaMovimiento), fechaNegocio);
    }

    public void publicarDebitoCarteraRechazado(
            String referenciaTipo,
            UUID referenciaId,
            UUID usuarioId,
            BigDecimal monto,
            String motivo,
            LocalDate fechaNegocio) {

        publicador.publicar(AGREGADO, usuarioId, "DebitoCarteraRechazado",
                new DebitoRechazadoPayload(referenciaTipo, referenciaId, usuarioId, monto, motivo), fechaNegocio);
    }
}
