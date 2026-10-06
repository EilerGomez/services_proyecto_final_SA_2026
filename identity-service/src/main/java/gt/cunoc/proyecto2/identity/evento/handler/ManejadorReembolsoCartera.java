package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.payload.MovimientoCarteraPayload;
import gt.cunoc.proyecto2.identity.evento.recibido.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Compensación: acredita el reembolso si existe el débito y publica CarteraReembolsada.
 */
@Component
public class ManejadorReembolsoCartera implements ManejadorEvento<ReembolsoCarteraSolicitado> {

    private static final Logger log = LoggerFactory.getLogger(ManejadorReembolsoCartera.class);

    private final MovimientoRepository movimientos;

    private final WalletService servicio;

    private final PublicadorEventos publicador;

    public ManejadorReembolsoCartera(
            MovimientoRepository movimientos, WalletService servicio, PublicadorEventos publicador) {
        this.movimientos = movimientos;
        this.servicio = servicio;
        this.publicador = publicador;
    }

    @Override
    public String tipoEvento() {
        return "ReembolsoCarteraSolicitado";
    }

    @Override
    public Class<ReembolsoCarteraSolicitado> tipoPayload() {
        return ReembolsoCarteraSolicitado.class;
    }

    @Override
    public void manejar(EventoRecibido<ReembolsoCarteraSolicitado> evento) {
        ReembolsoCarteraSolicitado solicitud = evento.payload();
        Optional<MovimientoCartera> debito =
                movimientos.findByTipoAndReferenciaId("DEBITO", solicitud.referenciaId());
        if (debito.isEmpty()) {
            log.warn("Reembolso sin débito previo, referencia {}", solicitud.referenciaId());
            return;
        }
        Optional<MovimientoCartera> previo =
                movimientos.findByTipoAndReferenciaId("REEMBOLSO", solicitud.referenciaId());
        if (previo.isPresent()) {
            publicar(solicitud, evento, previo.get());
            return;
        }
        Optional<MovimientoCartera> reembolso = servicio.solicitarReembolso(
                solicitud.usuarioId(), solicitud.monto(), solicitud.referenciaTipo(),
                solicitud.referenciaId(), solicitud.motivo(), evento.businessDate());
        reembolso.ifPresent(movimiento -> publicar(solicitud, evento, movimiento));
    }

    private void publicar(
            ReembolsoCarteraSolicitado solicitud, EventoRecibido<ReembolsoCarteraSolicitado> evento,
            MovimientoCartera movimiento) {
        publicador.publicar(
                "Cartera", solicitud.usuarioId(), "CarteraReembolsada",
                new MovimientoCarteraPayload(
                        movimiento.getId(), solicitud.referenciaTipo(), solicitud.referenciaId(),
                        solicitud.usuarioId(), movimiento.getMonto(),
                        movimiento.getSaldoResultante(), movimiento.getFechaMovimiento()),
                evento.businessDate());
    }
}
